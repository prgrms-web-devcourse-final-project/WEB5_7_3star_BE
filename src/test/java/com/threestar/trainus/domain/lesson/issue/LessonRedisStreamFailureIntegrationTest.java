package com.threestar.trainus.domain.lesson.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.doReturn;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ValueOperations;

import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonParticipantRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.testsupport.RedisStreamIntegrationTestSupport;

class LessonRedisStreamFailureIntegrationTest extends RedisStreamIntegrationTestSupport {

	@Autowired
	private LessonApplyProducer lessonApplyProducer;

	@Autowired
	private LessonWaitingRoomService waitingRoomService;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private LessonParticipantRepository lessonParticipantRepository;

	@Autowired
	private UserRepository userRepository;

	@SpyBean(name = "mqRedisTemplate")
	private StringRedisTemplate spiedMqRedisTemplate;

	@AfterEach
	void cleanDatabase() {
		lessonParticipantRepository.deleteAllInBatch();
		lessonRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Test
	@DisplayName("레슨 Redis Stream: 중복 신청은 waiting room과 재고를 오염시키지 않는다")
	void producerDuplicateRejectKeepsRedisStateStable() {
		User teacher = saveUser("dup-t@test.com", "dup-t");
		User student = saveUser("dup-s@test.com", "dup-s");
		Lesson lesson = saveOpenRunLesson(teacher.getId(), "lesson-dup", 2);

		String stockKey = LessonApplyStreamConstant.STOCK_PREFIX + lesson.getId();
		String duplicateKey = LessonApplyStreamConstant.DUPLICATE_PREFIX + lesson.getId();
		String waitingRoomKey = String.format(LessonApplyStreamConstant.WAITING_ROOM_KEY, lesson.getId());

		lessonApplyProducer.setStock(lesson.getId(), 2);

		String firstRequestId = lessonApplyProducer.send(lesson.getId(), student.getId());
		String duplicateResult = lessonApplyProducer.send(lesson.getId(), student.getId());

		assertThat(firstRequestId).isNotBlank();
		assertThat(duplicateResult).isEqualTo("ALREADY_APPLIED");
		assertThat(coreRedisTemplate.opsForValue().get(stockKey)).isEqualTo("1");
		assertThat(coreRedisTemplate.opsForSet().size(duplicateKey)).isEqualTo(1L);
		assertThat(coreRedisTemplate.opsForSet().isMember(duplicateKey, String.valueOf(student.getId()))).isTrue();
		assertThat(coreRedisTemplate.opsForZSet().size(waitingRoomKey)).isEqualTo(1L);
		assertThat(waitingRoomService.getRank(lesson.getId(), firstRequestId)).contains(1L);
		assertThat(mqRedisTemplate.opsForStream().size(LessonApplyStreamConstant.STREAM_KEY)).isZero();
	}

	@Test
	@DisplayName("레슨 Redis Stream: 재고가 없으면 pre-filter에서 되돌리고 대기열을 만들지 않는다")
	void producerStockExhaustionRollsBackRedisState() {
		User teacher = saveUser("empty-t@test.com", "empty-t");
		User student = saveUser("empty-s@test.com", "empty-s");
		Lesson lesson = saveOpenRunLesson(teacher.getId(), "lesson-empty", 1);

		String stockKey = LessonApplyStreamConstant.STOCK_PREFIX + lesson.getId();
		String duplicateKey = LessonApplyStreamConstant.DUPLICATE_PREFIX + lesson.getId();
		String waitingRoomKey = String.format(LessonApplyStreamConstant.WAITING_ROOM_KEY, lesson.getId());
		String statusKey = LessonApplyStreamConstant.STATUS_PREFIX + "missing";

		lessonApplyProducer.setStock(lesson.getId(), 0);

		String result = lessonApplyProducer.send(lesson.getId(), student.getId());

		assertThat(result).isNull();
		assertThat(coreRedisTemplate.opsForValue().get(stockKey)).isEqualTo("0");
		assertThat(coreRedisTemplate.hasKey(duplicateKey)).isFalse();
		assertThat(coreRedisTemplate.opsForZSet().size(waitingRoomKey)).isZero();
		assertThat(mqRedisTemplate.hasKey(statusKey)).isFalse();
		assertThat(mqRedisTemplate.opsForStream().size(LessonApplyStreamConstant.STREAM_KEY)).isZero();
	}

	@Test
	@DisplayName("레슨 Redis Stream: status 조회 실패 시 admission은 요청을 waiting room에 재등록한다")
	void admissionRequeuesWhenStatusLookupFails() {
		User teacher = saveUser("n-t@test.com", "n-t");
		User student = saveUser("n-s@test.com", "n-s");
		Lesson lesson = saveOpenRunLesson(teacher.getId(), "lesson-admit-null", 1);
		lessonApplyProducer.setStock(lesson.getId(), 1);

		String requestId = lessonApplyProducer.send(lesson.getId(), student.getId());
		String statusKey = LessonApplyStreamConstant.STATUS_PREFIX + requestId;
		String waitingRoomKey = String.format(LessonApplyStreamConstant.WAITING_ROOM_KEY, lesson.getId());

		ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
		doReturn(valueOperations).when(spiedMqRedisTemplate).opsForValue();
		when(valueOperations.multiGet(anyList())).thenReturn(null);

		LessonAdmissionScheduler admissionScheduler = new LessonAdmissionScheduler(
			coreRedisTemplate,
			spiedMqRedisTemplate,
			waitingRoomService
		);
		admissionScheduler.admitUsers();

		assertThat(waitingRoomService.getRank(lesson.getId(), requestId)).contains(1L);
		assertThat(coreRedisTemplate.opsForZSet().size(waitingRoomKey)).isEqualTo(1L);
		assertThat(coreRedisTemplate.opsForSet().isMember(LessonApplyStreamConstant.DIRTY_SET_KEY, String.valueOf(lesson.getId())))
			.isTrue();
		assertThat(mqRedisTemplate.hasKey(statusKey)).isTrue();
		assertThat(mqRedisTemplate.opsForStream().size(LessonApplyStreamConstant.STREAM_KEY)).isZero();
	}

	@Test
	@DisplayName("레슨 Redis Stream: admission pipeline 실패 시 dequeue된 요청을 재등록한다")
	void admissionRequeuesWhenPipelineFails() {
		User teacher = saveUser("e-t@test.com", "e-t");
		User student = saveUser("e-s@test.com", "e-s");
		Lesson lesson = saveOpenRunLesson(teacher.getId(), "lesson-admit-ex", 1);
		lessonApplyProducer.setStock(lesson.getId(), 1);

		String requestId = lessonApplyProducer.send(lesson.getId(), student.getId());
		String statusKey = LessonApplyStreamConstant.STATUS_PREFIX + requestId;
		String waitingRoomKey = String.format(LessonApplyStreamConstant.WAITING_ROOM_KEY, lesson.getId());

		doThrow(new RuntimeException("forced admission failure"))
			.when(spiedMqRedisTemplate)
			.executePipelined(any(SessionCallback.class));

		LessonAdmissionScheduler admissionScheduler = new LessonAdmissionScheduler(
			coreRedisTemplate,
			spiedMqRedisTemplate,
			waitingRoomService
		);
		admissionScheduler.admitUsers();

		assertThat(waitingRoomService.getRank(lesson.getId(), requestId)).contains(1L);
		assertThat(coreRedisTemplate.opsForZSet().size(waitingRoomKey)).isEqualTo(1L);
		assertThat(coreRedisTemplate.opsForSet().isMember(LessonApplyStreamConstant.DIRTY_SET_KEY, String.valueOf(lesson.getId())))
			.isTrue();
		assertThat(mqRedisTemplate.hasKey(statusKey)).isTrue();
		assertThat(mqRedisTemplate.opsForStream().size(LessonApplyStreamConstant.STREAM_KEY)).isZero();
	}

	private User saveUser(String email, String nickname) {
		return userRepository.save(User.builder()
			.email(email)
			.password("password")
			.nickname(nickname)
			.role(UserRole.USER)
			.build());
	}

	private Lesson saveOpenRunLesson(Long teacherId, String lessonName, int maxParticipants) {
		return lessonRepository.save(Lesson.builder()
			.lessonLeader(teacherId)
			.lessonName(lessonName)
			.description("lesson stream failure integration test")
			.maxParticipants(maxParticipants)
			.startAt(LocalDateTime.now().plusDays(1))
			.endAt(LocalDateTime.now().plusDays(1).plusHours(2))
			.price(0)
			.category(Category.GYM)
			.openTime(LocalDateTime.now().minusMinutes(5))
			.openRun(true)
			.city("서울시")
			.district("강남구")
			.dong("역삼동")
			.address("서울시 강남구 역삼동")
			.addressDetail("101호")
			.build());
	}
}
