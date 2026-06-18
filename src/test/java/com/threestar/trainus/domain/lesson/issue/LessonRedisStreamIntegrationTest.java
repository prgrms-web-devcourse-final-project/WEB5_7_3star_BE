package com.threestar.trainus.domain.lesson.issue;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonParticipantRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.testsupport.RedisStreamIntegrationTestSupport;

/*
 * 레슨 신청 Redis Stream의 대표 happy path를 실제 Redis/PostgreSQL 기반으로 검증하는 통합 테스트다.
 * producer enqueue, admission, consumer 처리, DB 반영, status 기록, stock reconciliation을 한 흐름으로 확인한다.
 */
class LessonRedisStreamIntegrationTest extends RedisStreamIntegrationTestSupport {

	// Docker/Testcontainers 연결 이슈는 RedisStreamIntegrationTestSupport에 정리되어 있다.
	@Autowired
	private LessonApplyProducer lessonApplyProducer;

	@Autowired
	private LessonApplyService lessonApplyService;

	@Autowired
	private LessonWaitingRoomService waitingRoomService;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private LessonParticipantRepository lessonParticipantRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@AfterEach
	void clearDatabase() {
		lessonParticipantRepository.deleteAllInBatch();
		lessonRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Test
	@DisplayName("레슨 Redis Stream 신청 E2E: producer, admission, consumer, stock reconciliation")
	void lessonApplyStreamEndToEnd() {
		User teacher = saveUser("teacher-lesson-it@test.com", "teacher-lesson-it", UserRole.USER);
		User student = saveUser("student-lesson-it@test.com", "student-lesson-it", UserRole.USER);
		Lesson lesson = lessonRepository.save(openRunLesson(teacher.getId()));
		lessonApplyProducer.setStock(lesson.getId(), 1);

		String requestId = lessonApplyProducer.send(lesson.getId(), student.getId());

		assertThat(requestId).isNotBlank();
		assertThat(waitingRoomService.getRank(lesson.getId(), requestId)).contains(1L);
		assertThat(coreRedisTemplate.opsForValue().get(LessonApplyStreamConstant.STOCK_PREFIX + lesson.getId()))
			.isEqualTo("0");

		new LessonAdmissionScheduler(coreRedisTemplate, mqRedisTemplate, waitingRoomService).admitUsers();
		createLessonConsumerGroup();

		MapRecord<String, String, String> admittedRecord = readSingleLessonStreamRecord();
		LessonApplyConsumer consumer = new LessonApplyConsumer(mqRedisTemplate, coreRedisTemplate, lessonApplyService);
		consumer.onMessage(admittedRecord);
		ReflectionTestUtils.invokeMethod(consumer, "processBuffer");

		assertThat(lessonParticipantRepository.existsByLessonIdAndUserId(lesson.getId(), student.getId())).isTrue();
		assertThat(mqRedisTemplate.opsForValue().get(LessonApplyStreamConstant.STATUS_PREFIX + requestId))
			.isEqualTo(LessonApplyStreamConstant.STATUS_SUCCESS);
		assertThat(mqRedisTemplate.opsForStream().size(LessonApplyStreamConstant.STREAM_KEY)).isZero();

		coreRedisTemplate.delete("lesson:busy:" + lesson.getId());
		coreRedisTemplate.delete("lesson:busy:last_active:" + lesson.getId());
		LessonStockReconciliationScheduler reconciliationScheduler = new LessonStockReconciliationScheduler(
			lessonRepository,
			lessonParticipantRepository,
			lessonApplyProducer,
			coreRedisTemplate,
			mqRedisTemplate
		);
		transactionTemplate.executeWithoutResult(status -> reconciliationScheduler.reconcileStock());

		Lesson reconciledLesson = lessonRepository.findById(lesson.getId()).orElseThrow();
		assertThat(reconciledLesson.getParticipantCount()).isEqualTo(1);
		assertThat(coreRedisTemplate.opsForValue().get(LessonApplyStreamConstant.STOCK_PREFIX + lesson.getId()))
			.isEqualTo("0");
		assertThat(coreRedisTemplate.opsForSet().isMember(
			LessonApplyStreamConstant.DIRTY_SET_KEY,
			String.valueOf(lesson.getId())
		)).isFalse();
	}

	@SuppressWarnings("unchecked")
	private MapRecord<String, String, String> readSingleLessonStreamRecord() {
		var records = mqRedisTemplate.opsForStream().range(LessonApplyStreamConstant.STREAM_KEY, Range.unbounded());
		assertThat(records).hasSize(1);

		MapRecord<String, Object, Object> record = records.get(0);
		Map<String, String> value = record.getValue()
			.entrySet()
			.stream()
			.collect(java.util.stream.Collectors.toMap(
				entry -> entry.getKey().toString(),
				entry -> entry.getValue().toString()
			));
		return StreamRecords.mapBacked(value)
			.withStreamKey(LessonApplyStreamConstant.STREAM_KEY)
			.withId(record.getId());
	}

	private void createLessonConsumerGroup() {
		try {
			mqRedisTemplate.opsForStream()
				.createGroup(LessonApplyStreamConstant.STREAM_KEY, ReadOffset.from("0-0"), LessonApplyStreamConstant.GROUP);
		} catch (RuntimeException e) {
			if (!hasBusyGroupMessage(e)) {
				throw e;
			}
		}
	}

	private boolean hasBusyGroupMessage(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			String message = current.getMessage();
			if (message != null && message.contains("BUSYGROUP")) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private User saveUser(String email, String nickname, UserRole role) {
		return userRepository.save(User.builder()
			.email(email)
			.password("password")
			.nickname(nickname)
			.role(role)
			.build());
	}

	private Lesson openRunLesson(Long teacherId) {
		return Lesson.builder()
			.lessonLeader(teacherId)
			.lessonName("Redis Stream Integration Lesson")
			.description("Integration test lesson")
			.maxParticipants(1)
			.startAt(LocalDateTime.now().plusDays(1))
			.endAt(LocalDateTime.now().plusDays(1).plusHours(2))
			.price(0)
			.category(Category.GYM)
			.openTime(LocalDateTime.now().minusMinutes(1))
			.openRun(true)
			.city("서울시")
			.district("강남구")
			.dong("역삼동")
			.address("서울시 강남구 역삼동")
			.addressDetail("101호")
			.build();
	}
}
