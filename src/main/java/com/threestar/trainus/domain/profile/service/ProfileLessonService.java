package com.threestar.trainus.domain.profile.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonStatus;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import com.threestar.trainus.domain.profile.dto.ProfileCreatedLessonListResponseDto;
import com.threestar.trainus.domain.profile.mapper.ProfileLessonMapper;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.utils.PageLimitCalculator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileLessonService {

	private final LessonRepository lessonRepository;
	private final UserService userService;

	// 특정 유저가 개설한 레슨 목록 조회
	@Transactional(readOnly = true)
	public ProfileCreatedLessonListResponseDto getUserCreatedLessons(
		Long userId, int page, int limit, LessonStatus status) {

		// User 존재 확인
		userService.getUserById(userId);

		// limit 계산
		int countLimit = PageLimitCalculator.calculatePageLimit(page, limit, 5);

		// 리스트 조회 (Native Query)
		List<Lesson> lessons = lessonRepository.findCreatedLessonsByUser(
			userId,
			status != null ? status.name() : null,
			limit,
			(page - 1) * limit // OFFSET
		);

		// totalCount 조회 (Native Query)
		int totalCount;
		if (status != null) {
			totalCount = lessonRepository.countCreatedLessonsByStatus(userId, status, countLimit);
		} else {
			totalCount = lessonRepository.countCreatedLessons(userId, countLimit);
		}

		// DTO 변환
		return ProfileLessonMapper.toProfileCreatedLessonListResponseDto(lessons, totalCount);
	}

}
