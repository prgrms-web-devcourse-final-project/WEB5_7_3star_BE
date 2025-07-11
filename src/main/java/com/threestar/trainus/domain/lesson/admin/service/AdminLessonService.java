package com.threestar.trainus.domain.lesson.admin.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.lesson.admin.dto.ApplicationProcessResponseDto;
import com.threestar.trainus.domain.lesson.admin.dto.CreatedLessonListResponseDto;
import com.threestar.trainus.domain.lesson.admin.dto.LessonApplicationListResponseDto;
import com.threestar.trainus.domain.lesson.admin.dto.LessonCreateRequestDto;
import com.threestar.trainus.domain.lesson.admin.dto.LessonResponseDto;
import com.threestar.trainus.domain.lesson.admin.dto.ParticipantListResponseDto;
import com.threestar.trainus.domain.lesson.admin.entity.ApplicationStatus;
import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.lesson.admin.entity.LessonApplication;
import com.threestar.trainus.domain.lesson.admin.entity.LessonImage;
import com.threestar.trainus.domain.lesson.admin.entity.LessonStatus;
import com.threestar.trainus.domain.lesson.admin.mapper.CreatedLessonMapper;
import com.threestar.trainus.domain.lesson.admin.mapper.LessonApplicationMapper;
import com.threestar.trainus.domain.lesson.admin.mapper.LessonMapper;
import com.threestar.trainus.domain.lesson.admin.mapper.LessonParticipantMapper;
import com.threestar.trainus.domain.lesson.admin.repository.LessonApplicationRepository;
import com.threestar.trainus.domain.lesson.admin.repository.LessonImageRepository;
import com.threestar.trainus.domain.lesson.admin.repository.LessonRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * 강사용(레슨 생성한 사람)만 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminLessonService {

	private final LessonRepository lessonRepository;           // 레슨 DB 접근
	private final LessonImageRepository lessonImageRepository; // 레슨 이미지 DB 접근
	private final LessonMapper lessonMapper;
	private final UserRepository userRepository;
	private final LessonApplicationRepository lessonApplicationRepository;
	private final LessonApplicationMapper lessonApplicationMapper; // 신청자 목록용 Mapper
	private final LessonParticipantMapper lessonParticipantMapper; // 참가자 목록용 mapper
	private final CreatedLessonMapper createdLessonMapper; //생성한 레슨 목록용 mapper

	// 새로운 레슨을 생성하는 메서드
	@Transactional
	public LessonResponseDto createLesson(LessonCreateRequestDto requestDto, Long userId) {
		// User 조회
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 동일 레슨 중복 검증(동일한 강사가 같은 이름+시간으로 레슨 생성 차단)
		boolean isDuplicate = lessonRepository.existsDuplicateLesson(
			userId,
			requestDto.lessonName(),
			requestDto.startAt()
		);
		if (isDuplicate) {
			throw new BusinessException(ErrorCode.DUPLICATE_LESSON);
		}

		// 시간 겹침 검증(같은 강사가 동일 시간대에 여러 레슨 생성 차단)
		boolean hasConflict = lessonRepository.hasTimeConflictLesson(
			userId,
			requestDto.startAt(),
			requestDto.endAt()
		);
		if (hasConflict) {
			throw new BusinessException(ErrorCode.LESSON_TIME_OVERLAP);
		}

		// User 엔티티로 레슨 생성
		Lesson lesson = lessonMapper.toEntity(requestDto, user);

		// 레슨 저장
		Lesson savedLesson = lessonRepository.save(lesson);

		// 레슨 이미지 저장
		List<LessonImage> savedImages = saveLessonImages(savedLesson, requestDto.lessonImages());

		// 응답 DTO 반환
		return lessonMapper.toResponseDto(savedLesson, savedImages);
	}

	// 레슨 이미지들을 db에 저장하는 메서드
	private List<LessonImage> saveLessonImages(Lesson lesson, List<String> imageUrls) {
		// 이미지가 없는 경우 빈 리스트 반환
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

	//레슨 삭제
	@Transactional
	public void deleteLesson(Long lessonId, Long userId) {
		// User 존재 확인
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		//레슨 조회
		Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(() -> new BusinessException(
			ErrorCode.LESSON_NOT_FOUND));

		//권한 확인 -> 레슨을 올린사람만 삭제가 가능하도록
		if (!lesson.getLessonLeader().equals(userId)) {
			throw new BusinessException(ErrorCode.LESSON_DELETE_FORBIDDEN);
		}

		//이미 삭제된 레슨인지 확인
		if (lesson.getDeletedAt() != null) {
			throw new BusinessException(ErrorCode.LESSON_ALREADY_DELETED);
		}

		lesson.lessonDelete();
		lessonRepository.save(lesson);
	}

	//레슨 신청자 목록 조회
	public LessonApplicationListResponseDto getLessonApplications(
		Long lessonId, int page, int limit, String status, Long userId) {

		// 레슨 존재 및 권한 확인
		Lesson lesson = validateLessonAccess(lessonId, userId);

		// 상태 파라미터 검증
		ApplicationStatus applicationStatus = validateStatus(status);

		// 페이징 설정
		Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());

		// 신청자 목록 조회
		Page<LessonApplication> applicationPage;
		if ("ALL".equals(status)) {
			applicationPage = lessonApplicationRepository.findByLesson(lesson, pageable);
		} else {
			applicationPage = lessonApplicationRepository.findByLessonAndStatus(lesson, applicationStatus, pageable);
		}

		//dto변환
		return lessonApplicationMapper.toListResponseDto(
			applicationPage.getContent(),
			applicationPage.getTotalElements()
		);
	}

	//레슨 신청 승인/거절 처리
	@Transactional
	public ApplicationProcessResponseDto processLessonApplication(
		Long lessonApplicationId, String action, Long userId) {

		// 신청 존재 확인
		LessonApplication application = lessonApplicationRepository.findById(lessonApplicationId)
			.orElseThrow(() -> new BusinessException(ErrorCode.LESSON_APPLICATION_NOT_FOUND));

		// 강사 권한 확인 -> 해당 레슨의 강사인지 확인
		Lesson lesson = application.getLesson();
		if (!lesson.getLessonLeader().equals(userId)) {
			throw new BusinessException(ErrorCode.LESSON_ACCESS_FORBIDDEN);
		}

		// 이미 처리된 신청인지 확인(대기중아니라면 -> 이미 승인이나 거절처리 된거니까)
		if (!application.getStatus().equals(ApplicationStatus.PENDING)) {
			throw new BusinessException(ErrorCode.LESSON_APPLICATION_ALREADY_PROCESSED);
		}

		//승인/거절 처리
		if ("APPROVED".equals(action)) {
			application.approve();
			// 승인 시 레슨 참가자수 증가
			lesson.incrementParticipantCount();
		} else if ("DENIED".equals(action)) {
			application.deny();
		} else {
			throw new BusinessException(ErrorCode.INVALID_APPLICATION_ACTION);
		}

		LessonApplication savedApplication = lessonApplicationRepository.save(application);

		// 응답 DTO 생성
		return ApplicationProcessResponseDto.builder()
			.lessonApplicationId(savedApplication.getId())
			.userId(savedApplication.getUser().getId())
			.status(savedApplication.getStatus().name())
			.processedAt(savedApplication.getUpdatedAt())
			.build();
	}

	//레슨 참가자 목록 조회(승인된 사람들만 있음)
	public ParticipantListResponseDto getLessonParticipants(
		Long lessonId, int page, int limit, Long userId) {

		// 레슨 존재 및 권한 확인
		Lesson lesson = validateLessonAccess(lessonId, userId);

		// 페이징 설정
		Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").ascending());

		// 승인처리(APPROVED)된 -> 레슨 신청자들만 조회
		Page<LessonApplication> participantPage = lessonApplicationRepository
			.findByLessonAndStatus(lesson, ApplicationStatus.APPROVED, pageable);

		// dto 변환
		return lessonParticipantMapper.toParticipantsResponseDto(
			participantPage.getContent(),
			participantPage.getTotalElements()
		);
	}

	//강사가 개설한 레슨 목록 조회
	public CreatedLessonListResponseDto getCreatedLessons(
		Long userId, int page, int limit, String status) {

		// User 존재 확인
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 페이징 설정
		Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());

		// 레슨 상태에 따른 조회
		Page<Lesson> lessonPage;
		if (status != null && !status.isEmpty()) {
			// 레슨 상태에 따라서 필터링
			LessonStatus lessonStatus = validateLessonStatus(status);
			lessonPage = lessonRepository.findByLessonLeaderAndStatusAndDeletedAtIsNull(userId, lessonStatus, pageable);
		} else {
			// 전체조회
			lessonPage = lessonRepository.findByLessonLeaderAndDeletedAtIsNull(userId, pageable);
		}

		// dto 변환
		return createdLessonMapper.toCreatedLessonListResponseDto(
			lessonPage.getContent(),
			lessonPage.getTotalElements()
		);
	}

	//레슨 접근 권한 검증 -> 올린사람(강사)가 맞는지 체크
	private Lesson validateLessonAccess(Long lessonId, Long userId) {
		// User 존재 확인
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 레슨 존재하는지 확인
		Lesson lesson = lessonRepository.findById(lessonId)
			.orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));

		// 삭제된 레슨 확인
		if (lesson.isDeleted()) {
			throw new BusinessException(ErrorCode.LESSON_NOT_FOUND);
		}

		// 강사 본인 확인
		if (!lesson.getLessonLeader().equals(userId)) {
			throw new BusinessException(ErrorCode.LESSON_ACCESS_FORBIDDEN);
		}

		return lesson;
	}

	//레슨 상태 검증
	private ApplicationStatus validateStatus(String status) {
		if ("ALL".equals(status)) {
			return null; // ALL인 경우 null 반환해서 필터링 안함
		}

		try {
			return ApplicationStatus.valueOf(status);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_APPLICATION_STATUS);
		}
	}

	//레슨 신청 상태 검증
	private LessonStatus validateLessonStatus(String status) {
		try {
			return LessonStatus.valueOf(status);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_LESSON_STATUS);
		}
	}

}

