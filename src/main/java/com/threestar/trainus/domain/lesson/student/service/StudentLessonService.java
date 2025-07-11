package com.threestar.trainus.domain.lesson.student.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.lesson.admin.entity.LessonImage;
import com.threestar.trainus.domain.lesson.admin.mapper.LessonMapper;
import com.threestar.trainus.domain.lesson.admin.repository.LessonImageRepository;
import com.threestar.trainus.domain.lesson.admin.repository.LessonRepository;
import com.threestar.trainus.domain.lesson.student.dto.LessonApplicationResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonDetailResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonSearchListResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonSearchResponseDto;
import com.threestar.trainus.domain.lesson.admin.entity.Category;
import com.threestar.trainus.domain.lesson.student.dto.PaginationDto;
import com.threestar.trainus.domain.metadata.dto.ProfileMetadataResponseDto;
import com.threestar.trainus.domain.metadata.service.ProfileMetadataService;
import com.threestar.trainus.domain.profile.entity.Profile;
import com.threestar.trainus.domain.profile.repository.ProfileRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentLessonService {

	private final LessonRepository lessonRepository;
	private final LessonImageRepository lessonImageRepository;
	private final LessonMapper lessonMapper;
	private final ProfileRepository profileRepository;
	private final UserRepository userRepository;
	private final ProfileMetadataService profileMetadataService;

	public LessonSearchListResponseDto getLessons(
		int page, int limit, String category, String search,
		String city, String district, String dong
	) {
		// 목데이터 반환중 TODO 구현 필요
		LessonSearchResponseDto lesson = new LessonSearchResponseDto(
			1L,
			"이동국의 축구교실",
			"이동국",
			"https://example.com/leader-image.jpg",
			2,
			24,
			4.5f,
			Category.FOOTBALL,
			40000,
			10,
			8,
			"모집중",
			LocalDateTime.parse("2025-07-04T10:00:00"),
			LocalDateTime.parse("2025-07-04T10:00:00"),
			LocalDateTime.parse("2025-07-04T10:00:00"),
			true,
			"경기도",
			"고양시 덕양구",
			"고양동",
			LocalDateTime.parse("2025-07-04T10:00:00"),
			List.of("https://example.com/image1.jpg", "https://example.com/image2.jpg")
		);

		List<LessonSearchResponseDto> lessonList = List.of(lesson);

		PaginationDto pagination = new PaginationDto(
			2,  // currentPage
			5,  // totalPages
			24, // totalCount
			5   // limit
		);

		return new LessonSearchListResponseDto(lessonList, pagination);
	}

	@Transactional
	public LessonDetailResponseDto getLessonDetail(Long lessonId) {

		// 레슨 조회
		Lesson lesson = lessonRepository.findById(lessonId)
			.orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));

		// 유저 조회
		User leader = userRepository.findById(lesson.getLessonLeader())
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 유저 프로필 조회
		Profile profile = profileRepository.findByUserId(leader.getId())
			.orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));

		// 프로필 메타데이터 조회
		ProfileMetadataResponseDto metadata = profileMetadataService.getMetadata(leader.getId());

		// 레슨 이미지 조회
		List<LessonImage> lessonImages = lessonImageRepository.findAllByLessonId(lesson.getId());
		List<String> imageUrls = lessonImages.stream()
			.map(LessonImage::getImageUrl)
			.toList();

		return lessonMapper.toLessonDetailDto(
			lesson,
			leader,
			profile,
			metadata.reviewCount(),
			metadata.rating(),
			imageUrls
		);
	}

	public LessonApplicationResponseDto applyToLesson(Long lessonId, Long userId) {
		return null;
	}
}
