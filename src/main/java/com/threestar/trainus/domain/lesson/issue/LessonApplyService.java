package com.threestar.trainus.domain.lesson.issue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Metrics;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonParticipant;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonParticipantRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonApplyService {

	private final LessonRepository lessonRepository;
	private final LessonParticipantRepository lessonParticipantRepository;
	private final UserService userService;
	private final StringRedisTemplate stringRedisTemplate;

	@Transactional
	public boolean apply(Long lessonId, Long userId, String requestId, Long produceTime) {
		String statusKey = LessonApplyStreamConstant.STATUS_PREFIX + requestId;
		try {
			// 멱등성 검증 (중복 신청 확인)
			if (lessonParticipantRepository.existsByLessonIdAndUserId(lessonId, userId)) {
				log.warn("Lesson apply already exists. lessonId={}, userId={}", lessonId, userId);
				updateStatus(statusKey, "SUCCESS");
				return true;
			}

			User user = userService.getUserById(userId);
			Lesson lesson = lessonRepository.findById(lessonId)
				.orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));

			// 시간 검증
			if (LocalDateTime.now().isAfter(lesson.getEndAt())) {
				log.warn("Lesson apply failed: Already closed. lessonId={}", lessonId);
				updateStatus(statusKey, "FAIL:CLOSED");
				return false;
			}

			// 실제 DB 저장 및 카운트 증가
			LessonParticipant participant = LessonParticipant.builder()
				.lesson(lesson)
				.user(user)
				.build();
			lessonParticipantRepository.save(participant);
			lesson.incrementParticipantCount();

			// 처리 성공 결과 저장 및 지연 시간 측정
			long latency = System.currentTimeMillis() - produceTime;
			log.info("Lesson apply success. requestId={}, latency={}ms", requestId, latency);

			// Micrometer 지표 기록
			Metrics.timer("lesson.apply.latency").record(latency, TimeUnit.MILLISECONDS);

			updateStatus(statusKey, "SUCCESS");

			return true;
		} catch (Exception e) {
			log.error("Failed to apply lesson in consumer: {}", e.getMessage());
			updateStatus(statusKey, "FAIL:ERROR");
			return false;
		}
	}

	private void updateStatus(String key, String status) {
		stringRedisTemplate.opsForValue()
			.set(key, status, Duration.ofMinutes(LessonApplyStreamConstant.STATUS_TTL_MINUTE));
	}
}
