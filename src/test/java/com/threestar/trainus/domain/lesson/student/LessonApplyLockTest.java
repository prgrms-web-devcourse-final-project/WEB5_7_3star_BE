package com.threestar.trainus.domain.lesson.student;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import com.threestar.trainus.domain.lesson.student.service.StudentLessonFacade;
import com.threestar.trainus.domain.lesson.student.service.StudentLessonService;
import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonParticipantRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
// @Transactional
public class LessonApplyLockTest {

	@Autowired
	private StudentLessonService lessonService;

	@Autowired
	private StudentLessonFacade lessonFacade;

	@Autowired
	private LessonParticipantRepository lessonParticipantRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private LessonRepository lessonRepository;

	private Long lessonId;
	private static final int MAX_PARTICIPANTS = 300;
	private static final int CONCURRENT_USERS = 10000;

	@BeforeEach
	void setUp() {
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
		// lessonId로 lesson 삭제
		lessonParticipantRepository.deleteByLessonId(lessonId);

		lessonRepository.deleteById(lessonId);

		// 생성된 유저들 삭제
		for (int i = 0; i < CONCURRENT_USERS; i++) {
			String email = "user" + i + "@test.com";
			userRepository.findByEmail(email).ifPresent(userRepository::delete);
		}
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

		log.info("총 소요 시간(ms): {}", stopWatch.getTotalTimeMillis());
		log.info("참가자 수 Count (엔티티 기준): {}", appliedCount);
		log.info("실제 승인된 참가자 수 Count (DB 기준): {}", approvedCount);
		log.info("성공 요청 수: {}", successCount.get());
		log.info("실패 요청 수: {}", failCount.get());

		Assertions.assertTrue(approvedCount == MAX_PARTICIPANTS, "정원 일치");
	}

	@Test
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
}