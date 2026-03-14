package com.threestar.trainus.domain.lesson.student;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import com.threestar.trainus.domain.lesson.issue.LessonApplyProducer;
import com.threestar.trainus.domain.lesson.student.service.StudentLessonFacade;
import com.threestar.trainus.domain.lesson.student.service.StudentLessonService;
import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonParticipantRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@org.springframework.test.context.ActiveProfiles({"local", "consumer"}) // 필요시 수정
public class LessonApplyLockTest {

	@Autowired
	private StudentLessonService lessonService;

	@Autowired
	private StudentLessonFacade lessonFacade;

	@Autowired
	private LessonApplyProducer lessonApplyProducer;

	@Autowired
	private LessonParticipantRepository lessonParticipantRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private com.threestar.trainus.domain.profile.repository.ProfileRepository profileRepository;

	@Autowired
	private com.threestar.trainus.domain.metadata.repository.ProfileMetadataRepository profileMetadataRepository;

	@Autowired
	private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

	private Long lessonId;
	private static final int MAX_PARTICIPANTS = 300;
	private static final int CONCURRENT_USERS = 10000;

	@BeforeEach
	void setUp() {
		// Redis Stream 메시지 비우기
		redisTemplate.opsForStream().trim(com.threestar.trainus.domain.lesson.issue.LessonApplyStreamConstant.STREAM_KEY, 0);

		// 기존 데이터 정리
		lessonParticipantRepository.deleteAllInBatch();
		lessonRepository.deleteAllInBatch();
		profileRepository.deleteAllInBatch();
		profileMetadataRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();

		// 테스트 전 사용자와 레슨 미리 셋업
		Lesson lesson = Lesson.builder()
			.lessonLeader(999L)
			.lessonName("Test Lesson")
			.description("Concurrent join test")
			.maxParticipants(MAX_PARTICIPANTS)
			.startAt(LocalDateTime.now().plusDays(1))
			.endAt(LocalDateTime.now().plusDays(2))
			.price(10000)
			.category(Category.GYM)
			.openTime(LocalDateTime.now())
			.openRun(true) // 선착순
			.city("경기도")
			.district("고양시")
			.dong("고양동")
			.address("경기도 고양시 고양동")
			.addressDetail("고양이 아파트")
			.build();
		lessonRepository.save(lesson);
		this.lessonId = lesson.getId();

		// 유저 생성
		for (int i = 0; i < CONCURRENT_USERS; i++) {
			User user = User.builder()
				.email("user" + i + "@test.com")
				.password("12341234")
				.nickname("user" + i)
				.role(UserRole.USER)
				.build();

			userRepository.save(user);
		}
	}

	@AfterEach
	void tearDown() {
		// 비동기 처리 완료 대기 및 데이터 정리
		try { Thread.sleep(500); } catch (InterruptedException ignored) {}

		// 레슨 참가자, 레슨, 유저 삭제
		lessonParticipantRepository.deleteAllInBatch();
		lessonRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();

		// Redis Stream 초기화
		redisTemplate.opsForStream().trim(com.threestar.trainus.domain.lesson.issue.LessonApplyStreamConstant.STREAM_KEY, 0);
	}

	@Test
	@DisplayName("동시 요청 시 - 락 미적용: 인원 수 초과 발생 가능")
	void applyToLesson_동시요청_락미적용_최대참가자수를초과함() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(300); // 병렬 쓰레드 수 조절
		CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);

		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failCount = new AtomicInteger();

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		for (int i = 0; i < CONCURRENT_USERS; i++) {
			final String email = "user" + i + "@test.com";

			executor.submit(() -> {
				try {
					Lesson updatedLesson = lessonRepository.findById(lessonId).orElseThrow();
					User user = userRepository.findByEmail(email).orElseThrow();
					lessonService.applyToLessonWithoutLock(lessonId, user.getId());
					log.info("신청 성공 - email: {} | participantCount: {}", email, updatedLesson.getParticipantCount());
					successCount.incrementAndGet();
				} catch (Exception e) {
					failCount.incrementAndGet();
					log.error("신청 실패 - email: {} | message: {}", email, e.getMessage());
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		stopWatch.stop();
		executor.shutdown();

		// 결과 출력
		long approvedCount = lessonParticipantRepository.countByLessonId(lessonId);
		Lesson latestLesson = lessonRepository.findById(lessonId).orElseThrow();
		long appliedCount = latestLesson.getParticipantCount();

		log.info("[락 미적용 테스트 결과]");
		log.info("총 소요 시간(ms): {}", stopWatch.getTotalTimeMillis());
		log.info("참가자 수 Count (엔티티 기준): {}", appliedCount);
		log.info("실제 승인된 참가자 수 Count (DB 기준): {}", approvedCount);
		log.info("성공 요청 수: {}", successCount.get());
		log.info("실패 요청 수: {}", failCount.get());

		Assertions.assertTrue(approvedCount >= MAX_PARTICIPANTS, "정원 초과 발생");
	}

	@Test
	@DisplayName("동시 요청 시 - 비관락 적용: 인원 수 초과 없이 정상 처리")
	void applyToLessonWithLock_동시요청_비관락적용_최대참가자수를초과하지않음() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(100); // 병렬 쓰레드 수 조절
		CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);

		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failCount = new AtomicInteger();

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		for (int i = 0; i < CONCURRENT_USERS; i++) {
			final String email = "user" + i + "@test.com";

			executor.submit(() -> {
				try {
					Lesson updatedLesson = lessonRepository.findById(lessonId).orElseThrow();
					User user = userRepository.findByEmail(email).orElseThrow();
					lessonService.applyToLessonWithPessimisticLock(lessonId, user.getId());
					log.info("신청 성공 - email: {} | participantCount: {}", email, updatedLesson.getParticipantCount());
					successCount.incrementAndGet();
				} catch (Exception e) {
					failCount.incrementAndGet();
					log.error("신청 실패 - email: {} | message: {}", email, e.getMessage());
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		stopWatch.stop();
		executor.shutdown();

		// 결과 출력
		long approvedCount = lessonParticipantRepository.countByLessonId(lessonId);
		Lesson latestLesson = lessonRepository.findById(lessonId).orElseThrow();
		long appliedCount = latestLesson.getParticipantCount();

		log.info("[비관적 락 테스트 결과]");
		log.info("총 소요 시간(ms): {}", stopWatch.getTotalTimeMillis());
		log.info("참가자 수 Count (엔티티 기준): {}", appliedCount);
		log.info("실제 승인된 참가자 수 Count (DB 기준): {}", approvedCount);
		log.info("성공 요청 수: {}", successCount.get());
		log.info("실패 요청 수: {}", failCount.get());

		Assertions.assertEquals(MAX_PARTICIPANTS, approvedCount, "정원 일치");
	}

	@Test
	@Disabled
	@DisplayName("동시 요청 시 - 분산락(Redis) 적용: 인원 수 초과 없이 정상 처리")
	void applyToLessonWithDistributedLock_동시요청_분산락적용_최대참가자수를초과하지않음() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(100); // 병렬 쓰레드 수 조절
		CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);

		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failCount = new AtomicInteger();

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		for (int i = 0; i < CONCURRENT_USERS; i++) {
			final String email = "user" + i + "@test.com";

			executor.submit(() -> {
				try {
					User user = userRepository.findByEmail(email).orElseThrow();
					lessonFacade.applyToLessonWithDistributedLock(lessonId, user.getId());
					successCount.incrementAndGet();
				} catch (Exception e) {
					failCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		stopWatch.stop();
		executor.shutdown();

		// 결과 출력
		long approvedCount = lessonParticipantRepository.countByLessonId(lessonId);
		Lesson latestLesson = lessonRepository.findById(lessonId).orElseThrow();
		long appliedCount = latestLesson.getParticipantCount();

		log.info("[분산락 테스트 결과]");
		log.info("총 소요 시간(ms): {}", stopWatch.getTotalTimeMillis());
		log.info("참가자 수 Count (엔티티 기준): {}", appliedCount);
		log.info("실제 승인된 참가자 수 Count (DB 기준): {}", approvedCount);
		log.info("성공 요청 수: {}", successCount.get());
		log.info("실패 요청 수: {}", failCount.get());

		Assertions.assertEquals(MAX_PARTICIPANTS, approvedCount, "정원이 정확히 일치해야 함");
	}

	@Test
	@DisplayName("동시 요청 시 - Redis Stream(MQ) 적용: 인원 수 초과 없이 정상 처리")
	void applyToLessonWithMQ_동시요청_비동기처리_최대참가자수를초과하지않음() throws InterruptedException {
		// Redis 재고 초기화
		lessonApplyProducer.setStock(lessonId, MAX_PARTICIPANTS);

		ExecutorService executor = Executors.newFixedThreadPool(100);
		CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);

		AtomicInteger producerSuccessCount = new AtomicInteger();
		AtomicInteger producerFailCount = new AtomicInteger();

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		// 비동기 요청 전송 (Producer)
		for (int i = 0; i < CONCURRENT_USERS; i++) {
			final String email = "user" + i + "@test.com";
			executor.submit(() -> {
				try {
					User user = userRepository.findByEmail(email).orElseThrow();
					boolean sent = lessonApplyProducer.send(lessonId, user.getId());
					if (sent) {
						producerSuccessCount.incrementAndGet();
					} else {
						producerFailCount.incrementAndGet();
					}
				} catch (Exception e) {
					producerFailCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		stopWatch.stop();
		executor.shutdown();

		log.info("[MQ Producer 완료] 요청 전송 소요 시간(ms): {}", stopWatch.getTotalTimeMillis());
		log.info("Producer 성공(선점): {}, 실패(매진): {}", producerSuccessCount.get(), producerFailCount.get());

		// Consumer 처리를 위한 대기 (비동기)
		log.info("Consumer 처리를 기다리는 중...");
		int waitCount = 0;
		long approvedCount = 0;
		while (waitCount < 30) { // 최대 30초 대기
			approvedCount = lessonParticipantRepository.countByLessonId(lessonId);
			if (approvedCount >= MAX_PARTICIPANTS) break;
			Thread.sleep(1000);
			waitCount++;
		}

		// 최종 결과 검증
		long finalApprovedCount = lessonParticipantRepository.countByLessonId(lessonId);
		Lesson latestLesson = lessonRepository.findById(lessonId).orElseThrow();
		long appliedCount = latestLesson.getParticipantCount();

		log.info("[MQ 테스트 최종 결과]");
		log.info("실제 승인된 참가자 수 (DB 기준): {}", finalApprovedCount);
		log.info("엔티티 카운트: {}", appliedCount);

		Assertions.assertEquals(MAX_PARTICIPANTS, finalApprovedCount, "정원이 정확히 일치해야 함");
	}
}
