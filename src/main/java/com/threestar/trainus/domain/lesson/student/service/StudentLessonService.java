package com.threestar.trainus.domain.lesson.student.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.threestar.trainus.domain.lesson.admin.entity.ApplicationStatus;
import com.threestar.trainus.domain.lesson.admin.entity.Category;
import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.lesson.admin.entity.LessonApplication;
import com.threestar.trainus.domain.lesson.admin.entity.LessonImage;
import com.threestar.trainus.domain.lesson.admin.entity.LessonParticipant;
import com.threestar.trainus.domain.lesson.admin.entity.LessonStatus;
import com.threestar.trainus.domain.lesson.admin.mapper.LessonMapper;
import com.threestar.trainus.domain.lesson.admin.repository.LessonApplicationRepository;
import com.threestar.trainus.domain.lesson.admin.repository.LessonImageRepository;
import com.threestar.trainus.domain.lesson.admin.repository.LessonParticipantRepository;
import com.threestar.trainus.domain.lesson.admin.repository.LessonRepository;
import com.threestar.trainus.domain.lesson.student.dto.LessonApplicationResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonDetailResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonSearchListResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonSearchResponseDto;
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
	private final ProfileRepository profileRepository;
	private final UserRepository userRepository;
	private final ProfileMetadataService profileMetadataService;
	private final LessonParticipantRepository lessonParticipantRepository;
	private final LessonApplicationRepository lessonApplicationRepository;

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

		int totalCount = 24;

		return new LessonSearchListResponseDto(lessonList, totalCount);
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

		return LessonMapper.toLessonDetailDto(
			lesson,
			leader,
			profile,
			metadata.reviewCount(),
			metadata.rating(),
			imageUrls
		);
	}

	@Transactional
	public LessonApplicationResponseDto applyToLesson(Long lessonId, Long userId) {
		// 레슨 조회
		Lesson lesson = lessonRepository.findById(lessonId)
			.orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));

		// 유저 조회
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 개설자 신청 불가 체크
		if (lesson.getLessonLeader().equals(userId)) {
			throw new BusinessException(ErrorCode.LESSON_CREATOR_CANNOT_APPLY);
		}

		// 중복 체크
		boolean alreadyParticipated = lessonParticipantRepository.existsByLessonIdAndUserId(lessonId, userId);
		boolean alreadyApplied = lessonApplicationRepository.existsByLessonIdAndUserId(lessonId, userId);
		if (alreadyParticipated || alreadyApplied) {
			throw new BusinessException(ErrorCode.ALREADY_APPLIED);
		}

		// 레슨 상태 체크
		if (lesson.getStatus() != LessonStatus.RECRUITING) {
			throw new BusinessException(ErrorCode.LESSON_NOT_AVAILABLE);
		}

		// 선착순 여부에 따라 저장 처리 분기
		if (lesson.getOpenRun()) {
			// 바로 참가자 등록, 인원수 증가 TODO 성능개선시 동시성 고려
			LessonParticipant participant = LessonParticipant.builder()
				.lesson(lesson)
				.user(user)
				.build();
			lessonParticipantRepository.save(participant);
			lesson.incrementParticipantCount();

			return LessonApplicationResponseDto.builder()
				.lessonApplicationId(participant.getId())
				.lessonId(lesson.getId())
				.userId(user.getId())
				.status(ApplicationStatus.APPROVED.name())
				.appliedAt(participant.getJoinAt())
				.build();
		} else {
			// 신청만 등록
			LessonApplication application = LessonApplication.builder()
				.lesson(lesson)
				.user(user)
				.build();
			lessonApplicationRepository.save(application);

			return LessonApplicationResponseDto.builder()
				.lessonApplicationId(application.getId())
				.lessonId(lesson.getId())
				.userId(user.getId())
				.status(ApplicationStatus.PENDING.name())
				.appliedAt(application.getCreatedAt())
				.build();
		}
	}

	@Transactional
	public void cancelLessonApplication(Long lessonId, Long userId) {
		// 레슨 조회
		Lesson lesson = lessonRepository.findById(lessonId)
			.orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));

		// 유저 조회
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 선착순 레슨은 신청 취소 불가 처리
		if (lesson.getOpenRun()) {
			throw new BusinessException(ErrorCode.CANNOT_CANCEL_APPROVED_APPLICATION);
		}

		// PENDING 상태의 LessonApplication 삭제
		LessonApplication application = lessonApplicationRepository.findByLessonIdAndUserId(lessonId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.LESSON_APPLICATION_NOT_FOUND));

		if (application.getStatus() != ApplicationStatus.PENDING) {
			throw new BusinessException(ErrorCode.CANNOT_CANCEL_APPROVED_APPLICATION);
		}

		lessonApplicationRepository.delete(application);
	}
}
