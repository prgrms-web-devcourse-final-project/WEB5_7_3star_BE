package com.threestar.trainus.domain.lesson.teacher.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.lesson.issue.LessonApplyProducer;
import com.threestar.trainus.domain.lesson.teacher.constants.LessonConstants.Participants;
import com.threestar.trainus.domain.lesson.teacher.constants.LessonConstants.Time;
import com.threestar.trainus.domain.lesson.teacher.dto.ApplicationProcessResponseDto;
import com.threestar.trainus.domain.lesson.teacher.dto.CreatedLessonListResponseDto;
import com.threestar.trainus.domain.lesson.teacher.dto.LessonApplicationListResponseDto;
import com.threestar.trainus.domain.lesson.teacher.dto.LessonCreateRequestDto;
import com.threestar.trainus.domain.lesson.teacher.dto.LessonResponseDto;
import com.threestar.trainus.domain.lesson.teacher.dto.LessonUpdateRequestDto;
import com.threestar.trainus.domain.lesson.teacher.dto.LessonUpdateResponseDto;
import com.threestar.trainus.domain.lesson.teacher.dto.ParticipantListResponseDto;
import com.threestar.trainus.domain.lesson.teacher.entity.ApplicationAction;
import com.threestar.trainus.domain.lesson.teacher.entity.ApplicationStatus;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonApplication;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonImage;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonParticipant;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonStatus;
import com.threestar.trainus.domain.lesson.teacher.mapper.CreatedLessonMapper;
import com.threestar.trainus.domain.lesson.teacher.mapper.LessonApplicationMapper;
import com.threestar.trainus.domain.lesson.teacher.mapper.LessonMapper;
import com.threestar.trainus.domain.lesson.teacher.mapper.LessonParticipantMapper;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonApplicationRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonImageRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonParticipantRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;
import com.threestar.trainus.global.utils.PageLimitCalculator;

import lombok.RequiredArgsConstructor;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * 강사용(레슨 생성한 사람)만 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminLessonService {

	private final LessonRepository lessonRepository;
	private final LessonImageRepository lessonImageRepository;
	private final LessonApplicationRepository lessonApplicationRepository;
	private final LessonParticipantRepository lessonParticipantRepository;
	private final UserService userService;
	private final LessonCreationLimitService lessonCreationLimitService;
	private final LessonApplyProducer lessonApplyProducer;
	private final GeometryFactory geometryFactory;

	//레슨 생성
	@Transactional
	public LessonResponseDto createLesson(LessonCreateRequestDto requestDto, Long userId) {
		User user = userService.getUserById(userId);

		// 생성시에 필요한 검증을 진행
		validateLessonCreation(requestDto, userId);

		//레슨 생성 제한 확인 및 쿨타임 설정
		lessonCreationLimitService.checkAndSetCreationLimit(userId);

		// Point 객체 생성
		Point point = geometryFactory.createPoint(
			new Coordinate(requestDto.longitude(), requestDto.latitude()));

		// 레슨 생성 및 저장
		Lesson lesson = LessonMapper.toEntity(requestDto, user, point);
		Lesson savedLesson = lessonRepository.save(lesson);

		// 이미지 저장
		List<LessonImage> savedImages = saveLessonImages(savedLesson, requestDto.lessonImages());

		// Redis 재고 동기화
		syncRedisStock(savedLesson);

		return LessonMapper.toResponseDto(savedLesson, savedImages);
	}

	//레슨 삭제
	@Transactional
	public void deleteLesson(Long lessonId, Long userId) {
		Lesson lesson = validateLessonAccess(lessonId, userId);
		validateLessonDeletion(lesson);

		lesson.lessonDelete();
		lessonRepository.save(lesson);
	}

	//레슨 수정
	@Transactional
	public LessonUpdateResponseDto updateLesson(Long lessonId, LessonUpdateRequestDto requestDto, Long userId) {
		Lesson lesson = validateLessonAccess(lessonId, userId);

		validateLessonIsRecruiting(lesson);
		validateLessonTimeLimit(lesson);
		validateUpdateRequest(requestDto);

		boolean hasParticipants = hasApprovedParticipants(lesson);

		// 수정 처리
		updateBasicInfo(lesson, requestDto);
		updateMaxParticipants(lesson, requestDto, hasParticipants);
		tryUpdateRestricted(lesson, requestDto, userId, lessonId, hasParticipants);

		// 저장 및 응답
		Lesson savedLesson = lessonRepository.save(lesson);
		List<LessonImage> updatedImages = updateLessonImages(savedLesson, requestDto.lessonImages());

		// Redis 재고 동기화
		syncRedisStock(savedLesson);

		return LessonMapper.toUpdateResponseDto(savedLesson, updatedImages);
	}

	@Transactional(readOnly = true)
	public LessonApplicationListResponseDto getLessonApplications(
		Long lessonId, int page, int limit, String status, Long userId) {

		Lesson lesson = validateLessonAccess(lessonId, userId);
		ApplicationStatus applicationStatus = toApplicationStatus(status);

		// offset, limit 계산
		int offset = (page - 1) * limit;
		int countLimit = PageLimitCalculator.calculatePageLimit(page, limit, 5);

		List<LessonApplication> applications;
		int total;

		if ("ALL".equals(status)) {
			// 모든 상태 조회
			List<Long> ids = lessonApplicationRepository.findIdsByLesson(
				lessonId, offset, limit
			);

			applications = ids.isEmpty()
				? Collections.emptyList()
				: lessonApplicationRepository.findAllWithUserProfileLesson(ids);

			total = lessonApplicationRepository.countAllByLesson(
				lesson.getId(), countLimit
			);
		} else {
			// 특정 상태 조회
			List<Long> ids = lessonApplicationRepository.findIdsByLessonAndStatus(
				lesson.getId(), applicationStatus.name(), offset, limit
			);

			applications = ids.isEmpty()
				? Collections.emptyList()
				: lessonApplicationRepository.findAllWithUserProfileLesson(ids);

			total = lessonApplicationRepository.countAllByLessonAndStatus(
				lesson.getId(), applicationStatus.name(), countLimit
			);
		}

		return LessonApplicationMapper.toListResponseDto(applications, total);
	}

	//레슨 신청 승인/거절 처리
	@Transactional
	public ApplicationProcessResponseDto processLessonApplication(
		Long lessonApplicationId, ApplicationAction action, Long userId) {

		LessonApplication application = findApplicationById(lessonApplicationId);
		Lesson lesson = application.getLesson();

		validateIsYourLesson(lesson, userId);
		validatePending(application);

		// 승인/거절 처리
		processApplication(application, lesson, action);

		LessonApplication savedApplication = lessonApplicationRepository.save(application);

		return buildApplicationProcessResponse(savedApplication);
	}

	@Transactional(readOnly = true)
	public ParticipantListResponseDto getLessonParticipants(
		Long lessonId, int page, int limit, Long userId) {

		// 레슨 접근 권한 검증
		Lesson lesson = validateLessonAccess(lessonId, userId);

		// offset & countLimit 계산
		int offset = (page - 1) * limit;
		int countLimit = PageLimitCalculator.calculatePageLimit(page, limit, 5);

		// 참가자 조회
		List<Long> ids = lessonParticipantRepository.findIdsByLesson(lessonId, offset, limit);

		List<LessonParticipant> participants = ids.isEmpty()
			? Collections.emptyList()
			: lessonParticipantRepository.findAllWithUserAndProfile(ids);

		// 전체 카운트 조회
		int totalCount = lessonParticipantRepository.countAllByLesson(
			lesson.getId(), countLimit
		);

		return LessonParticipantMapper.toParticipantsResponseDto(participants, totalCount);
	}

	@Transactional(readOnly = true)
	public CreatedLessonListResponseDto getCreatedLessons(
		Long userId, int page, int limit, String status) {

		// user 검증
		userService.getUserById(userId);

		// offset / countLimit 계산
		int offset = (page - 1) * limit;
		int countLimit = PageLimitCalculator.calculatePageLimit(page, limit, 5);

		List<Lesson> lessons;
		int total;

		if (status != null && !status.isEmpty()) {
			// 상태 값이 있는 경우
			lessons = lessonRepository.findCreatedLessonsByStatus(
				userId, LessonStatus.valueOf(status), offset, limit
			);
			total = lessonRepository.countCreatedLessonsByStatus(
				userId, LessonStatus.valueOf(status), countLimit
			);
		} else {
			// 상태 필터 없는 경우
			lessons = lessonRepository.findCreatedLessons(
				userId, offset, limit
			);
			total = lessonRepository.countCreatedLessons(
				userId, countLimit
			);
		}

		return CreatedLessonMapper.toCreatedLessonListResponseDto(lessons, total);
	}

	// 검증 메서드

	//레슨 생성 시 검증
	private void validateLessonCreation(LessonCreateRequestDto requestDto, Long userId) {
		validateLessonTimes(requestDto.startAt(), requestDto.endAt());
		validateMaxParticipantsByType(requestDto.maxParticipants(), requestDto.openRun());
		validateDuplicateLesson(userId, requestDto.lessonName(), requestDto.startAt());
		validateTimeConflict(userId, requestDto.startAt(), requestDto.endAt());
	}

	//레슨 삭제 검증
	private void validateLessonDeletion(Lesson lesson) {
		validateLessonIsRecruiting(lesson);

		if (hasApprovedParticipants(lesson)) {
			throw new BusinessException(ErrorCode.LESSON_DELETE_HAS_PARTICIPANTS);
		}

		validateLessonTimeLimit(lesson);
	}

	//레슨 접근 권한 검증
	private Lesson validateLessonAccess(Long lessonId, Long userId) {
		userService.getUserById(userId);
		Lesson lesson = findLessonById(lessonId);
		validateLessonNotDeleted(lesson);
		validateIsYourLesson(lesson, userId);
		return lesson;
	}

	//모집중 상태 검증
	private void validateLessonIsRecruiting(Lesson lesson) {
		if (lesson.getStatus() != LessonStatus.RECRUITING) {
			throw new BusinessException(ErrorCode.LESSON_NOT_EDITABLE);
		}
	}

	//수정/삭제는 12시 전만 가능하도록
	private void validateLessonTimeLimit(Lesson lesson) {
		LocalDateTime timeLimit = lesson.getStartAt().minusHours(Time.EDIT_DELETE_LIMIT_HOURS);
		if (LocalDateTime.now().isAfter(timeLimit)) {
			throw new BusinessException(ErrorCode.LESSON_TIME_LIMIT_EXCEEDED);
		}
	}

	//승인된 참가자 존재 여부 확인
	private boolean hasApprovedParticipants(Lesson lesson) {
		return lessonApplicationRepository
			.countByLessonAndStatus(lesson, ApplicationStatus.APPROVED) > 0;
	}

	//강사 권한 검증
	private void validateIsYourLesson(Lesson lesson, Long userId) {
		if (!lesson.getLessonLeader().equals(userId)) {
			throw new BusinessException(ErrorCode.ACCESS_FORBIDDEN);
		}
	}

	//레슨 삭제 여부 검증
	private void validateLessonNotDeleted(Lesson lesson) {
		if (lesson.isDeleted()) {
			throw new BusinessException(ErrorCode.LESSON_NOT_FOUND);
		}
	}

	//시간 검증
	private void validateLessonTimes(LocalDateTime startAt, LocalDateTime endAt) {
		LocalDateTime now = LocalDateTime.now();

		if (startAt.isBefore(now)) {
			throw new BusinessException(ErrorCode.LESSON_START_TIME_INVALID);
		}

		if (endAt.isBefore(startAt) || endAt.isEqual(startAt)) {
			throw new BusinessException(ErrorCode.LESSON_END_TIME_BEFORE_START);
		}
	}

	//참여방식에 따른 최대 인원 검증
	private void validateMaxParticipantsByType(Integer maxParticipants, Boolean openRun) {
		int maxLimit = openRun ? Participants.MAX_OPEN_RUN_PARTICIPANTS : Participants.MAX_NORMAL_PARTICIPANTS;
		if (maxParticipants > maxLimit) {
			throw new BusinessException(ErrorCode.LESSON_MAX_PARTICIPANTS_EXCEEDED);
		}
	}

	//중복 레슨 검증
	private void validateDuplicateLesson(Long userId, String lessonName, LocalDateTime startAt) {
		boolean isDuplicate = lessonRepository.existsDuplicateLesson(userId, lessonName, startAt);
		if (isDuplicate) {
			throw new BusinessException(ErrorCode.DUPLICATE_LESSON);
		}
	}

	//시간 겹침 검증
	private void validateTimeConflict(Long userId, LocalDateTime startAt, LocalDateTime endAt) {
		boolean hasConflict = lessonRepository.hasTimeConflictLesson(userId, startAt, endAt);
		if (hasConflict) {
			throw new BusinessException(ErrorCode.LESSON_TIME_OVERLAP);
		}
	}

	//정원 초과 검증
	private void validateCapacity(Lesson lesson) {
		if (lesson.getParticipantCount() >= lesson.getMaxParticipants()) {
			throw new BusinessException(ErrorCode.LESSON_MAX_PARTICIPANTS_EXCEEDED);
		}
	}

	//수정 요청 검증
	private void validateUpdateRequest(LessonUpdateRequestDto requestDto) {
		if (!requestDto.hasBasicInfoChanges() && !requestDto.hasRestrictedChanges()
			&& requestDto.maxParticipants() == null) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
		}
	}

	//신청 처리 가능 여부 검증
	private void validatePending(LessonApplication application) {
		if (!application.getStatus().equals(ApplicationStatus.PENDING)) {
			throw new BusinessException(ErrorCode.LESSON_APPLICATION_ALREADY_PROCESSED);
		}
	}

	//기본 정보 수정
	private void updateBasicInfo(Lesson lesson, LessonUpdateRequestDto requestDto) {
		lesson.updateLessonName(requestDto.lessonName());
		lesson.updateDescription(requestDto.description());
	}

	//최대 참가 인원 수정
	private void updateMaxParticipants(Lesson lesson, LessonUpdateRequestDto requestDto,
		boolean hasParticipants) {
		if (requestDto.maxParticipants() != null) {
			lesson.updateMaxParticipants(requestDto.maxParticipants(), hasParticipants);
		}
	}

	//제한된 필드 수정
	private void tryUpdateRestricted(Lesson lesson, LessonUpdateRequestDto requestDto,
		Long userId, Long lessonId, boolean hasParticipants) {

		if (!requestDto.hasRestrictedChanges()) {
			return; //제한된 필드 수정 요청이 없다면 아무것도 안함
		}

		if (hasParticipants) {
			throw new BusinessException(ErrorCode.LESSON_PARTICIPANTS_EXIST_RESTRICTION);
		}

		updateRestrictedFields(lesson, requestDto, userId, lessonId);
	}

	//제한된 필드 업데이트 실행
	private void updateRestrictedFields(Lesson lesson, LessonUpdateRequestDto requestDto, Long userId, Long lessonId) {
		validateTimeChanges(lesson, requestDto, userId, lessonId);

		lesson.updateCategory(requestDto.category());
		lesson.updatePrice(requestDto.price());
		lesson.updateLessonTime(requestDto.startAt(), requestDto.endAt());
		lesson.updateOpenTime(requestDto.openTime());
		lesson.updateOpenRun(requestDto.openRun());
		lesson.updateLocation(requestDto.city(), requestDto.district(), requestDto.dong(), requestDto.ri());
		lesson.updateAddress(requestDto.address());
		lesson.updateAddressDetail(requestDto.addressDetail());

		// 좌표 정보 업데이트
		if (requestDto.latitude() != null && requestDto.longitude() != null) {
			Point point = geometryFactory.createPoint(
				new Coordinate(requestDto.longitude(), requestDto.latitude()));
			lesson.updateLocationPoint(point);
		}
	}

	//시간 변경 검증
	private void validateTimeChanges(Lesson lesson, LessonUpdateRequestDto requestDto, Long userId,
		Long lessonId) {
		if (!requestDto.hasTimeChanges()) {
			return;
		}

		LocalDateTime newStartAt = requestDto.startAt() != null ? requestDto.startAt() : lesson.getStartAt();
		LocalDateTime newEndAt = requestDto.endAt() != null ? requestDto.endAt() : lesson.getEndAt();

		validateLessonTimes(newStartAt, newEndAt);

		boolean hasConflict = lessonRepository.hasTimeConflictForUpdate(userId, newStartAt, newEndAt, lessonId);
		if (hasConflict) {
			throw new BusinessException(ErrorCode.LESSON_TIME_OVERLAP);
		}
	}

	//신청 처리
	private void processApplication(LessonApplication application, Lesson lesson, ApplicationAction action) {
		if (action == ApplicationAction.APPROVED) {
			validateCapacity(lesson);
			application.approve();
			LessonParticipant participant = LessonParticipant.builder()
				.lesson(lesson)
				.user(application.getUser())
				.build();
			lessonParticipantRepository.save(participant);

			lesson.incrementParticipantCount();
		} else if (action == ApplicationAction.DENIED) {
			application.deny();
		}
	}

	//신청 상태 파싱
	private ApplicationStatus toApplicationStatus(String status) {
		if ("ALL".equals(status)) {
			return null;
		}
		try {
			return ApplicationStatus.valueOf(status);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_APPLICATION_STATUS);
		}
	}

	//레슨 이미지 저장
	private List<LessonImage> saveLessonImages(Lesson lesson, List<String> imageUrls) {
		if (imageUrls == null || imageUrls.isEmpty()) {
			return List.of();
		}

		List<LessonImage> lessonImages = imageUrls.stream()
			.map(url -> LessonImage.builder()
				.lesson(lesson)
				.imageUrl(url)
				.build())
			.toList();

		return lessonImageRepository.saveAll(lessonImages);
	}

	//레슨 이미지 업데이트
	private List<LessonImage> updateLessonImages(Lesson lesson, List<String> newImageUrls) {
		if (newImageUrls != null) {
			List<LessonImage> existingImages = lessonImageRepository.findByLesson(lesson);
			lessonImageRepository.deleteAll(existingImages);

			if (!newImageUrls.isEmpty()) {
				return saveLessonImages(lesson, newImageUrls);
			}
			return List.of();
		} else {
			return lessonImageRepository.findByLesson(lesson);
		}
	}

	//신청처리 응답dto
	private ApplicationProcessResponseDto buildApplicationProcessResponse(LessonApplication application) {
		return ApplicationProcessResponseDto.builder()
			.lessonApplicationId(application.getId())
			.userId(application.getUser().getId())
			.status(application.getStatus())
			.processedAt(application.getUpdatedAt())
			.build();
	}

	//레슨 조회
	public Lesson findLessonById(Long lessonId) {
		return lessonRepository.findById(lessonId)
			.orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));
	}

	// develop 브랜치에서 추가된 메서드 - Lock 기능 추가
	public Lesson findLessonByIdWithLock(Long lessonId) {
		return lessonRepository.findByIdWithLock(lessonId)
			.orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));
	}

	//레슨 신청 조회
	public LessonApplication findApplicationById(Long applicationId) {
		return lessonApplicationRepository.findById(applicationId)
			.orElseThrow(() -> new BusinessException(ErrorCode.LESSON_APPLICATION_NOT_FOUND));
	}

	// 수동으로 레슨 재고를 Redis와 동기화
	@Transactional(readOnly = true)
	public void syncLessonStockToRedis(Long lessonId) {
		Lesson lesson = findLessonById(lessonId);
		syncRedisStock(lesson);
	}

	// 레슨의 정원, 참여 방식 체크 후 Redis 재고 업데이트
	private void syncRedisStock(Lesson lesson) {
		if (lesson.getOpenRun()) {
			int stock = lesson.getMaxParticipants() - lesson.getParticipantCount();
			lessonApplyProducer.setStock(lesson.getId(), stock);
		}
	}
}
