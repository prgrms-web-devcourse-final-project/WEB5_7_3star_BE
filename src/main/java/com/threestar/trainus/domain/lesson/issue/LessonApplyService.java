package com.threestar.trainus.domain.lesson.issue;

import java.time.LocalDateTime;

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

	@Transactional
	public boolean apply(Long lessonId, Long userId) {
		try {
			// 멱등성 검증 (중복 신청 확인)
			if (lessonParticipantRepository.existsByLessonIdAndUserId(lessonId, userId)) {
				log.warn("Lesson apply already exists. lessonId={}, userId={}", lessonId, userId);
				return true;
			}

			User user = userService.getUserById(userId);
			Lesson lesson = lessonRepository.findById(lessonId)
				.orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));

			// 시간 검증
			if (LocalDateTime.now().isAfter(lesson.getEndAt())) {
				log.warn("Lesson apply failed: Already closed. lessonId={}", lessonId);
				return false;
			}

			// 실제 DB 저장 및 카운트 증가
			LessonParticipant participant = LessonParticipant.builder()
				.lesson(lesson)
				.user(user)
				.build();
			lessonParticipantRepository.save(participant);
			lesson.incrementParticipantCount();

			return true;
		} catch (Exception e) {
			log.error("Failed to apply lesson in consumer: {}", e.getMessage());
			return false;
		}
	}
}
