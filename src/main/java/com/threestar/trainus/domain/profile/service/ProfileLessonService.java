package com.threestar.trainus.domain.profile.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonStatus;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import com.threestar.trainus.domain.profile.dto.ProfileCreatedLessonListResponseDto;
import com.threestar.trainus.domain.profile.mapper.ProfileLessonMapper;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.domain.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileLessonService {

	private final LessonRepository lessonRepository;
	private final UserRepository userRepository;
	private final UserService userService;

	// 특정 유저가 개설한 레슨 목록 조회
	@Transactional(readOnly = true)
	public ProfileCreatedLessonListResponseDto getUserCreatedLessons(
		Long userId, int page, int limit, LessonStatus status) {

		// User 존재 확인
		User user = userService.getUserById(userId);

		// 페이징 설정 -> 내림차순!!
		Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());

		// 레슨 상태에 따른 조회
		Page<Lesson> lessonPage;
		if (status != null) {
			// 상태에 따라 조회 가능
			lessonPage = lessonRepository.findByLessonLeaderAndStatusAndDeletedAtIsNull(userId, status, pageable);
		} else {
			// 삭제되지 않은 레슨만 조회
			lessonPage = lessonRepository.findByLessonLeaderAndDeletedAtIsNull(userId, pageable);
		}

		// DTO 변환
		return ProfileLessonMapper.toProfileCreatedLessonListResponseDto(
			lessonPage.getContent(),
			lessonPage.getTotalElements()
		);
	}
}
