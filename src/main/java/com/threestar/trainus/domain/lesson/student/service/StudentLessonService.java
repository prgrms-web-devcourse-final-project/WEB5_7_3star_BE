package com.threestar.trainus.domain.lesson.student.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.lesson.student.dto.LessonApplicationResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonDetailResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonSearchListResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonSearchResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonSimpleResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.MyLessonApplicationListResponseDto;
import com.threestar.trainus.domain.lesson.student.entity.LessonSortType;
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
import com.threestar.trainus.domain.lesson.teacher.entity.ParticipantStatus;
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
import com.threestar.trainus.global.annotation.DistributedLock;
import com.threestar.trainus.global.utils.PageLimitCalculator;

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

	@Transactional(readOnly = true)
	public LessonSearchListResponseDto searchLessons(
		int page, int pageSize,
		Category category, String search,
		String city, String district, String dong, String ri,
		LessonSortType sortBy
	) {
		if (sortBy == null) {
			throw new BusinessException(ErrorCode.INVALID_SORT);
		}

		// offset/limit 계산
		int offset = (page - 1) * pageSize;
		int countLimit = PageLimitCalculator.calculatePageLimit(page, pageSize, 5);

		// 카테고리 ALL 처리
		String categoryValue = (category != null && !category.name().equalsIgnoreCase("ALL"))
			? category.name() : null;

		// Lesson 목록 조회 (검색어 여부에 따라 분기)
		List<Lesson> lessons = (search != null && !search.isEmpty())
			? lessonRepository.findLessonsWithFullText(
			categoryValue, city, district, dong, ri, search, sortBy.name(), offset, pageSize
		)
			: lessonRepository.findLessonsWithoutFullText(
			categoryValue, city, district, dong, ri, sortBy.name(), offset, pageSize
		);

		// count 조회 (검색어 여부에 따라 분기)
		int total = (search != null && !search.isEmpty())
			? lessonRepository.countLessonsWithFullText(
			categoryValue, city, district, dong, ri, search, countLimit
		)
			: lessonRepository.countLessonsWithoutFullText(
			categoryValue, city, district, dong, ri, countLimit
		);

		// DTO 매핑
		List<LessonSearchResponseDto> lessonDtos = lessons.stream()
			.map(lesson -> {
				User leader = userService.getUserById(lesson.getLessonLeader());
				Profile profile = profileRepository.findByUserId(leader.getId())
					.orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
				ProfileMetadataResponseDto metadata = profileMetadataService.getMetadata(leader.getId());

				List<String> imageUrls = lessonImageRepository.findAllByLessonId(lesson.getId()).stream()
					.map(LessonImage::getImageUrl)
					.toList();

				return LessonSearchMapper.toLessonSearchResponseDto(lesson, leader, profile, metadata, imageUrls);
			})
			.toList();

		return new LessonSearchListResponseDto(lessonDtos, total);
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
	public LessonApplicationResponseDto applyToLessonWithoutLock(Long lessonId, Long userId) {
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
			// 신청 시간 체크
			if (java.time.LocalDateTime.now().isBefore(lesson.getOpenTime())) {
				throw new BusinessException(ErrorCode.LESSON_NOT_YET_OPEN);
			}
			// 바로 참가자 등록, 인원수 증가
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
	@DistributedLock(key = "'lesson_apply:' + #lessonId")
	public LessonApplicationResponseDto applyToLessonWithDistributedLock(Long lessonId, Long userId) {
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
			// 락 내부에서 정원 체크
			if (lesson.getParticipantCount() >= lesson.getMaxParticipants()) {
				throw new BusinessException(ErrorCode.LESSON_NOT_AVAILABLE);
			}
			// 신청 시간 체크
			if (java.time.LocalDateTime.now().isBefore(lesson.getOpenTime())) {
				throw new BusinessException(ErrorCode.LESSON_NOT_YET_OPEN);
			}

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
	public LessonApplicationResponseDto applyToLessonWithPessimisticLock(Long lessonId, Long userId) {
		// 레슨 조회 (비관적 락)
		Lesson lesson = lessonRepository.findByIdWithLock(lessonId)
			.orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));

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
			// 락 내부에서 정원 체크
			if (lesson.getParticipantCount() >= lesson.getMaxParticipants()) {
				throw new BusinessException(ErrorCode.LESSON_NOT_AVAILABLE);
			}
			// 신청 시간 체크
			if (java.time.LocalDateTime.now().isBefore(lesson.getOpenTime())) {
				throw new BusinessException(ErrorCode.LESSON_NOT_YET_OPEN);
			}

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

	@Transactional(readOnly = true)
	public MyLessonApplicationListResponseDto getMyLessonApplications(
		Long userId, int page, int limit, String statusStr
	) {
		// status enum 변환
		ApplicationStatus status = null;
		if (!"ALL".equalsIgnoreCase(statusStr)) {
			try {
				status = ApplicationStatus.valueOf(statusStr.toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new BusinessException(ErrorCode.INVALID_APPLICATION_STATUS);
			}
		}

		// offset 계산
		int offset = (page - 1) * limit;

		// count limit 계산 (5개씩 이동 기준)
		int countLimit = PageLimitCalculator.calculatePageLimit(page, limit, 5);

		// 목록 조회
		List<Long> ids = lessonApplicationRepository.findIdsByUserAndStatus(
			userId,
			status != null ? status.name() : null,
			offset,
			limit
		);

		List<LessonApplication> applications = ids.isEmpty()
			? Collections.emptyList()
			: lessonApplicationRepository.findAllWithFetchJoin(ids);

		// count 조회 (최대 countLimit까지만 계산)
		int total = lessonApplicationRepository.countByUserAndStatus(
			userId,
			status != null ? status.name() : null,
			countLimit
		);

		return LessonApplicationMapper.toDtoListWithCount(applications, total);
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
		Optional<LessonParticipant> participant = lessonParticipantRepository
			.findByLessonIdAndUserIdAndStatus(
				lesson.getId(),
				user.getId(),
				ParticipantStatus.PAYMENT_PENDING
			);

		if (participant.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_LESSON_PARTICIPANT);
		}
	}

	@Transactional
	public void completeParticipantPayment(Long lessonId, Long userId) {
		LessonParticipant participant = lessonParticipantRepository
			.findByLessonIdAndUserIdAndStatus(lessonId, userId, ParticipantStatus.PAYMENT_PENDING)
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LESSON_PARTICIPANT));

		participant.completePayment();
		lessonParticipantRepository.save(participant);
	}
}
