package com.threestar.trainus.domain.lesson.student.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.threestar.trainus.domain.lesson.student.dto.LessonApplicationResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonDetailResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonSearchListResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonSearchResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonSimpleResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.MyLessonApplicationListResponseDto;
import com.threestar.trainus.domain.lesson.student.mapper.LessonApplicationMapper;
import com.threestar.trainus.domain.lesson.student.mapper.LessonApplyMapper;
import com.threestar.trainus.domain.lesson.student.mapper.LessonSearchMapper;
import com.threestar.trainus.domain.lesson.student.mapper.LessonSimpleMapper;
import com.threestar.trainus.domain.lesson.teacher.entity.ApplicationStatus;
import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonApplication;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonImage;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonParticipant;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonStatus;
import com.threestar.trainus.domain.lesson.teacher.mapper.LessonMapper;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonApplicationRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonImageRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonParticipantRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import com.threestar.trainus.domain.lesson.teacher.service.AdminLessonService;
import com.threestar.trainus.domain.metadata.dto.ProfileMetadataResponseDto;
import com.threestar.trainus.domain.metadata.service.ProfileMetadataService;
import com.threestar.trainus.domain.profile.entity.Profile;
import com.threestar.trainus.domain.profile.repository.ProfileRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.service.UserService;
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
	private final UserService userService;
	private final AdminLessonService adminLessonService;
	private final ProfileMetadataService profileMetadataService;
	private final LessonParticipantRepository lessonParticipantRepository;
	private final LessonApplicationRepository lessonApplicationRepository;

	@Transactional
	public LessonSearchListResponseDto searchLessons(
		int page, int limit,
		Category category, String search,
		String city, String district, String dong
	) {
		Pageable pageable = PageRequest.of(page - 1, limit);

		Category categoryEnum = null;
		if (!category.name().equalsIgnoreCase("ALL")) {
			try {
				categoryEnum = category;
			} catch (IllegalArgumentException e) {
				throw new BusinessException(ErrorCode.INVALID_CATEGORY);
			}
		}

		Page<Lesson> lessonPage = lessonRepository.findBySearchConditions(
			categoryEnum, city, district, dong, search, pageable
		);

		// 응답 DTO 리스트 매핑
		List<LessonSearchResponseDto> lessonDtos = lessonPage.getContent().stream()
			.map(lesson -> {
				// 개설자 정보 조회
				User leader = userService.getUserById(lesson.getLessonLeader());

				// 프로필 이미지
				Profile profile = profileRepository.findByUserId(leader.getId())
					.orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
				/*
				 * TODO: 프로필 공통예외처리 분리
				 *  */
				// 리뷰 개수, 평점 등 메타데이터
				ProfileMetadataResponseDto metadata = profileMetadataService.getMetadata(leader.getId());

				// 이미지 URL 목록
				List<String> imageUrls = lessonImageRepository.findAllByLessonId(lesson.getId()).stream()
					.map(LessonImage::getImageUrl)
					.toList();

				return LessonSearchMapper.toLessonSearchResponseDto(lesson, leader, profile, metadata, imageUrls);
			})
			.toList();

		return new LessonSearchListResponseDto(lessonDtos, (int)lessonPage.getTotalElements());
	}

	@Transactional
	public LessonDetailResponseDto getLessonDetail(Long lessonId) {

		// 레슨 조회
		Lesson lesson = adminLessonService.findLessonById(lessonId);

		// 유저 조회
		User leader = userService.getUserById(lesson.getLessonLeader());

		// 유저 프로필 조회
		Profile profile = profileRepository.findByUserId(leader.getId())
			.orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
		/*
		 * TODO: 프로필 공통 예외처리 분리
		 *  */
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
		Lesson lesson = adminLessonService.findLessonById(lessonId);

		// 유저 조회
		User user = userService.getUserById(userId);

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

			return LessonApplyMapper.toLessonApplicationResponseDto(
				lesson.getId(),
				user.getId(),
				ApplicationStatus.APPROVED,
				participant.getJoinAt()
			);
		} else {
			// 신청만 등록
			LessonApplication application = LessonApplication.builder()
				.lesson(lesson)
				.user(user)
				.build();
			lessonApplicationRepository.save(application);

			return LessonApplyMapper.toLessonApplicationResponseDto(
				lesson.getId(),
				user.getId(),
				ApplicationStatus.PENDING,
				application.getCreatedAt()
			);
		}
	}

	@Transactional
	public LessonApplicationResponseDto applyToLessonWithLock(Long lessonId, Long userId) {
		Lesson lesson = adminLessonService.findLessonByIdWithLock(lessonId); // 락적용 find 메서드

		User user = userService.getUserById(userId);

		if (lesson.getLessonLeader().equals(userId)) {
			throw new BusinessException(ErrorCode.LESSON_CREATOR_CANNOT_APPLY);
		}

		boolean alreadyParticipated = lessonParticipantRepository.existsByLessonIdAndUserId(lessonId, userId);
		boolean alreadyApplied = lessonApplicationRepository.existsByLessonIdAndUserId(lessonId, userId);
		if (alreadyParticipated || alreadyApplied) {
			throw new BusinessException(ErrorCode.ALREADY_APPLIED);
		}

		if (lesson.getStatus() != LessonStatus.RECRUITING) {
			throw new BusinessException(ErrorCode.LESSON_NOT_AVAILABLE);
		}

		if (lesson.getOpenRun()) {
			LessonParticipant participant = LessonParticipant.builder()
				.lesson(lesson)
				.user(user)
				.build();
			lessonParticipantRepository.save(participant);
			lesson.incrementParticipantCount();

			return LessonApplyMapper.toLessonApplicationResponseDto(
				lesson.getId(),
				user.getId(),
				ApplicationStatus.APPROVED,
				participant.getJoinAt()
			);
		} else {
			LessonApplication application = LessonApplication.builder()
				.lesson(lesson)
				.user(user)
				.build();
			lessonApplicationRepository.save(application);

			return LessonApplyMapper.toLessonApplicationResponseDto(
				lesson.getId(),
				user.getId(),
				ApplicationStatus.PENDING,
				application.getCreatedAt()
			);
		}
	}


	@Transactional
	public void cancelLessonApplication(Long lessonId, Long userId) {
		// 레슨 조회
		Lesson lesson = adminLessonService.findLessonById(lessonId);

		// 유저 조회
		userService.validateUserExists(userId);

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

	@Transactional
	public MyLessonApplicationListResponseDto getMyLessonApplications(Long userId, int page, int limit,
		String statusStr) {
		// status enum 변환
		ApplicationStatus status = null;
		if (!statusStr.equalsIgnoreCase("ALL")) {
			//status value 검증
			try {
				status = ApplicationStatus.valueOf(statusStr.toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new BusinessException(ErrorCode.INVALID_APPLICATION_STATUS);
			}
		}

		// 페이징 정렬
		Pageable pageable = PageRequest.of(page - 1, limit);

		// 신청 내역 조회
		Page<LessonApplication> applicationPage = (status == null)
			? lessonApplicationRepository.findByUserId(userId, pageable)
			: lessonApplicationRepository.findByUserIdAndStatus(userId, status, pageable);

		// DTO 변환
		return LessonApplicationMapper.toDtoListWithCount(
			applicationPage.getContent(),
			(int)applicationPage.getTotalElements()
		);
	}

	@Transactional
	public LessonSimpleResponseDto getLessonSimple(Long lessonId) {
		// 레슨 검증
		Lesson lesson = adminLessonService.findLessonById(lessonId);

		return LessonSimpleMapper.toLessonSimpleDto(lesson);
	}

	@Transactional
	public void cancelPayment(Long lessonId, Long userId) {
		Lesson lesson = adminLessonService.findLessonById(lessonId);
		lesson.decrementParticipantCount();
		lessonRepository.save(lesson);

		LessonParticipant lessonParticipant = lessonParticipantRepository.findByLessonIdAndUserId(lessonId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LESSON_PARTICIPANT));

		lessonParticipantRepository.delete(lessonParticipant);
	}

	@Transactional
	public void checkValidLessonParticipant(Lesson lesson, User user) {
		boolean ifExists = lessonParticipantRepository.existsByLessonIdAndUserId(lesson.getId(), user.getId());
		if (!ifExists) {
			throw new BusinessException(ErrorCode.INVALID_LESSON_PARTICIPANT);
		}
	}
}
