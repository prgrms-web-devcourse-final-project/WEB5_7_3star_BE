package com.threestar.trainus.domain.lesson.issue;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.transaction.support.TransactionTemplate;

import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonParticipant;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonStatus;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonParticipantRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import com.threestar.trainus.testsupport.RedisStreamIntegrationTestSupport;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;

@DisplayName("레슨 재고 보정 스케줄러 통합 테스트")
class LessonStockReconciliationSchedulerIntegrationTest extends RedisStreamIntegrationTestSupport {

	@Autowired
	private LessonApplyProducer lessonApplyProducer;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private LessonParticipantRepository lessonParticipantRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@AfterEach
	void cleanDatabase() {
		lessonParticipantRepository.deleteAllInBatch();
		lessonRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Test
	@DisplayName("대기열과 스트림이 비어 있으면 실제 DB 기준으로 레슨 수량과 Redis 재고를 보정한다")
	void reconcileStock_updatesLessonAndRedisWhenNoBacklogExists() {
		prepareEmptyStreamGroup();

		Lesson lesson = saveLesson("보정 대상 레슨", 2);
		User first = saveUser("reconcile-1@test.com", "reconcile-1");
		User second = saveUser("reconcile-2@test.com", "reconcile-2");
		saveParticipant(lesson, first);
		saveParticipant(lesson, second);

		String stockKey = LessonApplyStreamConstant.STOCK_PREFIX + lesson.getId();
		coreRedisTemplate.opsForValue().set(stockKey, "9");
		coreRedisTemplate.opsForSet().add(LessonApplyStreamConstant.DIRTY_SET_KEY, String.valueOf(lesson.getId()));

		transactionTemplate.executeWithoutResult(status -> createScheduler().reconcileStock());

		Lesson reconciledLesson = lessonRepository.findById(lesson.getId()).orElseThrow();
		assertThat(reconciledLesson.getParticipantCount()).isEqualTo(2);
		assertThat(reconciledLesson.getStatus()).isEqualTo(LessonStatus.RECRUITMENT_COMPLETED);
		assertThat(coreRedisTemplate.opsForValue().get(stockKey)).isEqualTo("0");
		assertThat(coreRedisTemplate.opsForSet().isMember(
			LessonApplyStreamConstant.DIRTY_SET_KEY,
			String.valueOf(lesson.getId())
		)).isFalse();
	}

	@Test
	@DisplayName("대기열에 남은 요청이 있으면 보정하지 않고 dirty set을 유지한다")
	void reconcileStock_skipsWhenWaitingRoomHasMessages() {
		prepareEmptyStreamGroup();

		Lesson lesson = saveLesson("대기열 잔여 레슨", 3);
		coreRedisTemplate.opsForValue().set(LessonApplyStreamConstant.STOCK_PREFIX + lesson.getId(), "7");
		coreRedisTemplate.opsForSet().add(LessonApplyStreamConstant.DIRTY_SET_KEY, String.valueOf(lesson.getId()));
		coreRedisTemplate.opsForZSet().add(
			String.format(LessonApplyStreamConstant.WAITING_ROOM_KEY, lesson.getId()),
			"request-1",
			1.0
		);

		transactionTemplate.executeWithoutResult(status -> createScheduler().reconcileStock());

		Lesson untouchedLesson = lessonRepository.findById(lesson.getId()).orElseThrow();
		assertThat(untouchedLesson.getParticipantCount()).isEqualTo(0);
		assertThat(untouchedLesson.getStatus()).isEqualTo(LessonStatus.RECRUITING);
		assertThat(coreRedisTemplate.opsForValue().get(LessonApplyStreamConstant.STOCK_PREFIX + lesson.getId()))
			.isEqualTo("7");
		assertThat(coreRedisTemplate.opsForSet().isMember(
			LessonApplyStreamConstant.DIRTY_SET_KEY,
			String.valueOf(lesson.getId())
		)).isTrue();
	}

	@Test
	@DisplayName("음수 재고가 감지되면 보정하지 않고 dirty set을 유지한다")
	void reconcileStock_skipsWhenRedisStockIsNegative() {
		prepareEmptyStreamGroup();

		Lesson lesson = saveLesson("음수 재고 레슨", 2);
		coreRedisTemplate.opsForValue().set(LessonApplyStreamConstant.STOCK_PREFIX + lesson.getId(), "-1");
		coreRedisTemplate.opsForSet().add(LessonApplyStreamConstant.DIRTY_SET_KEY, String.valueOf(lesson.getId()));

		transactionTemplate.executeWithoutResult(status -> createScheduler().reconcileStock());

		Lesson untouchedLesson = lessonRepository.findById(lesson.getId()).orElseThrow();
		assertThat(untouchedLesson.getParticipantCount()).isEqualTo(0);
		assertThat(untouchedLesson.getStatus()).isEqualTo(LessonStatus.RECRUITING);
		assertThat(coreRedisTemplate.opsForValue().get(LessonApplyStreamConstant.STOCK_PREFIX + lesson.getId()))
			.isEqualTo("-1");
		assertThat(coreRedisTemplate.opsForSet().isMember(
			LessonApplyStreamConstant.DIRTY_SET_KEY,
			String.valueOf(lesson.getId())
		)).isTrue();
	}

	@Test
	@DisplayName("스트림 backlog가 있으면 보정하지 않고 dirty set을 유지한다")
	void reconcileStock_skipsWhenStreamBacklogExists() {
		prepareStreamWithBacklog();

		Lesson lesson = saveLesson("스트림 backlog 레슨", 2);
		coreRedisTemplate.opsForValue().set(LessonApplyStreamConstant.STOCK_PREFIX + lesson.getId(), "5");
		coreRedisTemplate.opsForSet().add(LessonApplyStreamConstant.DIRTY_SET_KEY, String.valueOf(lesson.getId()));

		transactionTemplate.executeWithoutResult(status -> createScheduler().reconcileStock());

		Lesson untouchedLesson = lessonRepository.findById(lesson.getId()).orElseThrow();
		assertThat(untouchedLesson.getParticipantCount()).isEqualTo(0);
		assertThat(untouchedLesson.getStatus()).isEqualTo(LessonStatus.RECRUITING);
		assertThat(coreRedisTemplate.opsForValue().get(LessonApplyStreamConstant.STOCK_PREFIX + lesson.getId()))
			.isEqualTo("5");
		assertThat(coreRedisTemplate.opsForSet().isMember(
			LessonApplyStreamConstant.DIRTY_SET_KEY,
			String.valueOf(lesson.getId())
		)).isTrue();
	}

	@Test
	@DisplayName("busy key가 최근 활동 상태이면 보정하지 않고 dirty set을 유지한다")
	void reconcileStock_skipsWhenBusyStateIsRecent() {
		prepareEmptyStreamGroup();

		Lesson lesson = saveLesson("busy 레슨", 2);
		String busyKey = "lesson:busy:" + lesson.getId();
		String lastActiveKey = "lesson:busy:last_active:" + lesson.getId();
		coreRedisTemplate.opsForValue().set(LessonApplyStreamConstant.STOCK_PREFIX + lesson.getId(), "6");
		coreRedisTemplate.opsForValue().set(busyKey, "1");
		coreRedisTemplate.opsForValue().set(lastActiveKey, String.valueOf(System.currentTimeMillis()));
		coreRedisTemplate.opsForSet().add(LessonApplyStreamConstant.DIRTY_SET_KEY, String.valueOf(lesson.getId()));

		transactionTemplate.executeWithoutResult(status -> createScheduler().reconcileStock());

		Lesson untouchedLesson = lessonRepository.findById(lesson.getId()).orElseThrow();
		assertThat(untouchedLesson.getParticipantCount()).isEqualTo(0);
		assertThat(untouchedLesson.getStatus()).isEqualTo(LessonStatus.RECRUITING);
		assertThat(coreRedisTemplate.opsForValue().get(LessonApplyStreamConstant.STOCK_PREFIX + lesson.getId()))
			.isEqualTo("6");
		assertThat(coreRedisTemplate.opsForSet().isMember(
			LessonApplyStreamConstant.DIRTY_SET_KEY,
			String.valueOf(lesson.getId())
		)).isTrue();
	}

	private void prepareEmptyStreamGroup() {
		String streamKey = LessonApplyStreamConstant.STREAM_KEY;
		boolean hasStream = Boolean.TRUE.equals(mqRedisTemplate.hasKey(streamKey));

		if (!hasStream) {
			mqRedisTemplate.opsForStream().add(
				StreamRecords.mapBacked(Map.of("seed", "1")).withStreamKey(streamKey)
			);
		}

		try {
			mqRedisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0-0"), LessonApplyStreamConstant.GROUP);
		} catch (RuntimeException e) {
			if (!hasBusyGroupMessage(e)) {
				throw e;
			}
		}

		mqRedisTemplate.opsForStream().trim(streamKey, 0);
	}

	private void prepareStreamWithBacklog() {
		String streamKey = LessonApplyStreamConstant.STREAM_KEY;
		mqRedisTemplate.opsForStream().add(
			StreamRecords.mapBacked(Map.of("seed", "1")).withStreamKey(streamKey)
		);

		try {
			mqRedisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0-0"), LessonApplyStreamConstant.GROUP);
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

	private LessonStockReconciliationScheduler createScheduler() {
		return new LessonStockReconciliationScheduler(
			lessonRepository,
			lessonParticipantRepository,
			lessonApplyProducer,
			coreRedisTemplate,
			mqRedisTemplate
		);
	}

	private Lesson saveLesson(String lessonName, int maxParticipants) {
		return lessonRepository.saveAndFlush(Lesson.builder()
			.lessonLeader(100L)
			.lessonName(lessonName)
			.description("레슨 재고 보정 통합 테스트")
			.maxParticipants(maxParticipants)
			.startAt(LocalDateTime.now().plusDays(1))
			.endAt(LocalDateTime.now().plusDays(1).plusHours(2))
			.price(10_000)
			.category(Category.GYM)
			.openTime(LocalDateTime.now().minusMinutes(10))
			.openRun(true)
			.city("서울시")
			.district("강남구")
			.dong("역삼동")
			.address("서울시 강남구 역삼동")
			.addressDetail("101호")
			.build());
	}

	private User saveUser(String email, String nickname) {
		return userRepository.saveAndFlush(User.builder()
			.email(email)
			.password("password")
			.nickname(nickname)
			.role(UserRole.USER)
			.build());
	}

	private void saveParticipant(Lesson lesson, User user) {
		lessonParticipantRepository.saveAndFlush(LessonParticipant.builder()
			.lesson(lesson)
			.user(user)
			.build());
	}
}
