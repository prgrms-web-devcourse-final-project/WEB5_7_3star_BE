package com.threestar.trainus.domain.lesson.teacher.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.test.util.ReflectionTestUtils;

import com.threestar.trainus.domain.lesson.issue.LessonApplyProducer;
import com.threestar.trainus.domain.lesson.teacher.dto.CreatedLessonListResponseDto;
import com.threestar.trainus.domain.lesson.teacher.dto.LessonApplicationListResponseDto;
import com.threestar.trainus.domain.lesson.teacher.dto.LessonCreateRequestDto;
import com.threestar.trainus.domain.lesson.teacher.dto.LessonUpdateRequestDto;
import com.threestar.trainus.domain.lesson.teacher.dto.ParticipantListResponseDto;
import com.threestar.trainus.domain.lesson.teacher.dto.LessonResponseDto;
import com.threestar.trainus.domain.lesson.teacher.entity.ApplicationAction;
import com.threestar.trainus.domain.lesson.teacher.entity.ApplicationStatus;
import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonApplication;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonParticipant;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonStatus;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonApplicationRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonImageRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonParticipantRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

@ExtendWith(MockitoExtension.class)
public class AdminLessonServiceTest {
	@Mock
	private LessonRepository lessonRepository;

	@Mock
	private LessonImageRepository lessonImageRepository;

	@Mock
	private LessonApplicationRepository lessonApplicationRepository;

	@Mock
	private UserService userService;

	@InjectMocks
	private AdminLessonService adminLessonService;

	@Mock
	private LessonParticipantRepository lessonParticipantRepository;

	@Mock
	private LessonCreationLimitService lessonCreationLimitService;

	@Mock
	private LessonApplyProducer lessonApplyProducer;

	@Mock
	private GeometryFactory geometryFactory;

	@Test
	@DisplayName("정상적인 레슨 생성 테스트")
	void createLesson_Success() {
		Long userId = 1L;
		LocalDateTime now = LocalDateTime.now();

		LessonCreateRequestDto request = new LessonCreateRequestDto(
			"테스트 레슨",
			"레슨 설명",
			Category.GYM,
			30000,
			10,
			now.plusDays(1),
			now.plusDays(1).plusHours(2),
			null,
			false,
			"서울시",
			"강남구",
			"역삼동",
			"",
			"테스트 주소",
			"상세 주소",
			37.5665,
			126.9780,
			List.of()
		);

		User user = User.builder()
			.id(userId)
			.email("test@test.com")
			.nickname("테스트유저")
			.role(UserRole.USER)
			.build();

		Lesson savedLesson = Lesson.builder()
			.lessonLeader(userId)
			.lessonName("테스트 레슨")
			.description("레슨 설명")
			.category(Category.GYM)
			.price(30000)
			.maxParticipants(10)
			.startAt(now.plusDays(1))
			.endAt(now.plusDays(1).plusHours(2))
			.openRun(false)
			.city("서울시")
			.district("강남구")
			.dong("역삼동")
			.addressDetail("테스트 주소")
			.build();

		given(userService.getUserById(userId)).willReturn(user);
		given(lessonRepository.existsDuplicateLesson(anyLong(), anyString(), any(LocalDateTime.class)))
			.willReturn(false);
		given(lessonRepository.hasTimeConflictLesson(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
			.willReturn(false);
		given(lessonRepository.save(any(Lesson.class))).willReturn(savedLesson);

		willDoNothing().given(lessonCreationLimitService).checkAndSetCreationLimit(userId);

		LessonResponseDto response = adminLessonService.createLesson(request, userId);

		assertThat(response).isNotNull();
		assertThat(response.lessonName()).isEqualTo("테스트 레슨");
		assertThat(response.lessonLeader()).isEqualTo(userId);
		verify(lessonRepository).save(any(Lesson.class));

		verify(lessonCreationLimitService).checkAndSetCreationLimit(userId);
	}

	@Test
	@DisplayName("시작 시간이 과거인 경우 예외 발생")
	void createLesson_InvalidStartTime() {
		Long userId = 1L;
		LocalDateTime pastTime = LocalDateTime.now().minusHours(1);

		LessonCreateRequestDto request = new LessonCreateRequestDto(
			"테스트 레슨",
			"레슨 설명",
			Category.GYM,
			30000,
			10,
			pastTime, // 과거 시간
			pastTime.plusHours(2),
			null,
			false,
			"서울시",
			"강남구",
			"역삼동",
			"",
			"테스트 주소",
			"상세 주소",
			37.5665,
			126.9780,
			List.of()
		);

		User user = User.builder()
			.id(userId)
			.email("test@test.com")
			.nickname("테스트유저")
			.role(UserRole.USER)
			.build();

		given(userService.getUserById(userId)).willReturn(user);

		assertThatThrownBy(() -> adminLessonService.createLesson(request, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LESSON_START_TIME_INVALID);
	}

	@Test
	@DisplayName("중복 레슨 생성시 예외 발생")
	void createLesson_DuplicateLesson() {
		Long userId = 1L;
		LocalDateTime now = LocalDateTime.now();

		LessonCreateRequestDto request = new LessonCreateRequestDto(
			"테스트 레슨",
			"레슨 설명",
			Category.GYM,
			30000,
			10,
			now.plusDays(1),
			now.plusDays(1).plusHours(2),
			null,
			false,
			"서울시",
			"강남구",
			"역삼동",
			"",
			"테스트 주소",
			"상세 주소",
			37.5665,
			126.9780,
			List.of()
		);

		User user = User.builder()
			.id(userId)
			.email("test@test.com")
			.nickname("테스트유저")
			.role(UserRole.USER)
			.build();

		given(userService.getUserById(userId)).willReturn(user);
		given(lessonRepository.existsDuplicateLesson(anyLong(), anyString(), any(LocalDateTime.class)))
			.willReturn(true); // 중복 레슨 존재

		assertThatThrownBy(() -> adminLessonService.createLesson(request, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_LESSON);
	}

	@Test
	@DisplayName("종료 시간이 시작 시간보다 빠르거나 같으면 레슨 생성시 예외 발생")
	void createLesson_EndTimeBeforeStart() {
		Long userId = 1L;
		LocalDateTime startAt = LocalDateTime.now().plusDays(1);
		LessonCreateRequestDto request = createLessonRequest(startAt, startAt, false, 10, List.of());

		given(userService.getUserById(userId)).willReturn(createUser(userId));

		assertThatThrownBy(() -> adminLessonService.createLesson(request, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LESSON_END_TIME_BEFORE_START);

		verify(lessonRepository, never()).save(any(Lesson.class));
	}

	@Test
	@DisplayName("참여 방식별 최대 인원을 초과하면 레슨 생성시 예외 발생")
	void createLesson_MaxParticipantsExceeded() {
		Long userId = 1L;
		LocalDateTime startAt = LocalDateTime.now().plusDays(1);
		LessonCreateRequestDto request = createLessonRequest(startAt, startAt.plusHours(2), false, 101, List.of());

		given(userService.getUserById(userId)).willReturn(createUser(userId));

		assertThatThrownBy(() -> adminLessonService.createLesson(request, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LESSON_MAX_PARTICIPANTS_EXCEEDED);

		verify(lessonRepository, never()).save(any(Lesson.class));
	}

	@Test
	@DisplayName("같은 강사의 시간대가 겹치면 레슨 생성시 예외 발생")
	void createLesson_TimeConflict() {
		Long userId = 1L;
		LocalDateTime startAt = LocalDateTime.now().plusDays(1);
		LessonCreateRequestDto request = createLessonRequest(startAt, startAt.plusHours(2), false, 10, List.of());

		given(userService.getUserById(userId)).willReturn(createUser(userId));
		given(lessonRepository.existsDuplicateLesson(userId, request.lessonName(), request.startAt())).willReturn(false);
		given(lessonRepository.hasTimeConflictLesson(userId, request.startAt(), request.endAt())).willReturn(true);

		assertThatThrownBy(() -> adminLessonService.createLesson(request, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LESSON_TIME_OVERLAP);

		verify(lessonRepository, never()).save(any(Lesson.class));
	}

	@Test
	@DisplayName("선착순 레슨 생성 성공시 이미지를 저장하고 Redis 재고를 동기화한다")
	void createLesson_OpenRunSavesImagesAndSyncsStock() {
		Long userId = 1L;
		LocalDateTime startAt = LocalDateTime.now().plusDays(1);
		List<String> imageUrls = List.of("https://cdn.test/1.png", "https://cdn.test/2.png");
		LessonCreateRequestDto request = createLessonRequest(startAt, startAt.plusHours(2), true, 10, imageUrls);
		Point point = new GeometryFactory().createPoint(new Coordinate(request.longitude(), request.latitude()));
		Lesson savedLesson = createLesson(userId, startAt);
		ReflectionTestUtils.setField(savedLesson, "id", 100L);
		ReflectionTestUtils.setField(savedLesson, "openRun", true);

		given(userService.getUserById(userId)).willReturn(createUser(userId));
		given(lessonRepository.existsDuplicateLesson(userId, request.lessonName(), request.startAt())).willReturn(false);
		given(lessonRepository.hasTimeConflictLesson(userId, request.startAt(), request.endAt())).willReturn(false);
		willDoNothing().given(lessonCreationLimitService).checkAndSetCreationLimit(userId);
		given(geometryFactory.createPoint(any(Coordinate.class))).willReturn(point);
		given(lessonRepository.save(any(Lesson.class))).willReturn(savedLesson);
		given(lessonImageRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

		LessonResponseDto response = adminLessonService.createLesson(request, userId);

		assertThat(response.lessonImages()).containsExactlyElementsOf(imageUrls);
		verify(lessonImageRepository).saveAll(argThat(images -> {
			int count = 0;
			for (var ignored : images) {
				count++;
			}
			return count == 2;
		}));
		verify(lessonApplyProducer).setStock(100L, 10);
	}

	@Test
	@DisplayName("레슨 삭제 성공")
	void deleteLesson_Success() {
		Long lessonId = 1L;
		Long userId = 1L;
		LocalDateTime futureTime = LocalDateTime.now().plusDays(1);

		Lesson lesson = Lesson.builder()
			.lessonLeader(userId)
			.lessonName("테스트 레슨")
			.description("테스트 설명")
			.category(Category.GYM)
			.price(30000)
			.maxParticipants(10)
			.startAt(futureTime)
			.endAt(futureTime.plusHours(2))
			.openRun(false)
			.city("서울시")
			.district("강남구")
			.dong("역삼동")
			.addressDetail("테스트 주소")
			.build();

		given(userService.getUserById(userId)).willReturn(User.builder()
			.id(userId)
			.email("test@test.com")
			.nickname("테스트유저")
			.role(UserRole.USER)
			.build());
		given(lessonRepository.findById(lessonId)).willReturn(Optional.of(lesson));
		given(lessonApplicationRepository.countByLessonAndStatus(eq(lesson), eq(ApplicationStatus.APPROVED)))
			.willReturn(0);
		given(lessonRepository.save(any(Lesson.class))).willReturn(lesson);

		adminLessonService.deleteLesson(lessonId, userId);

		verify(lessonRepository).save(lesson);
		assertThat(lesson.isDeleted()).isTrue();
	}

	@Test
	@DisplayName("다른 사용자의 레슨 삭제시 예외 발생")
	void deleteLesson_AccessForbidden() {
		Long lessonId = 1L;
		Long userId = 1L;
		Long otherUserId = 2L;
		LocalDateTime futureTime = LocalDateTime.now().plusDays(1);

		Lesson lesson = Lesson.builder()
			.lessonLeader(otherUserId) // 다른 사용자의 레슨
			.lessonName("테스트 레슨")
			.description("테스트 설명")
			.category(Category.GYM)
			.price(30000)
			.maxParticipants(10)
			.startAt(futureTime)
			.endAt(futureTime.plusHours(2))
			.openRun(false)
			.city("서울시")
			.district("강남구")
			.dong("역삼동")
			.addressDetail("테스트 주소")
			.build();

		given(userService.getUserById(userId)).willReturn(User.builder()
			.id(userId)
			.email("test@test.com")
			.nickname("테스트유저")
			.role(UserRole.USER)
			.build());
		given(lessonRepository.findById(lessonId)).willReturn(Optional.of(lesson));

		assertThatThrownBy(() -> adminLessonService.deleteLesson(lessonId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_FORBIDDEN);
	}

	@Test
	@DisplayName("승인된 참가자가 있으면 레슨 삭제시 예외 발생")
	void deleteLesson_HasApprovedParticipants() {
		Long lessonId = 1L;
		Long userId = 1L;
		Lesson lesson = createLesson(userId, LocalDateTime.now().plusDays(2));

		given(userService.getUserById(userId)).willReturn(createUser(userId));
		given(lessonRepository.findById(lessonId)).willReturn(Optional.of(lesson));
		given(lessonApplicationRepository.countByLessonAndStatus(lesson, ApplicationStatus.APPROVED)).willReturn(1);

		assertThatThrownBy(() -> adminLessonService.deleteLesson(lessonId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LESSON_DELETE_HAS_PARTICIPANTS);

		verify(lessonRepository, never()).save(any(Lesson.class));
	}

	@Test
	@DisplayName("삭제 가능 시간 제한을 넘기면 레슨 삭제시 예외 발생")
	void deleteLesson_TimeLimitExceeded() {
		Long lessonId = 1L;
		Long userId = 1L;
		Lesson lesson = createLesson(userId, LocalDateTime.now().plusHours(10));

		given(userService.getUserById(userId)).willReturn(createUser(userId));
		given(lessonRepository.findById(lessonId)).willReturn(Optional.of(lesson));
		given(lessonApplicationRepository.countByLessonAndStatus(lesson, ApplicationStatus.APPROVED)).willReturn(0);

		assertThatThrownBy(() -> adminLessonService.deleteLesson(lessonId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LESSON_TIME_LIMIT_EXCEEDED);

		verify(lessonRepository, never()).save(any(Lesson.class));
	}

	@Test
	@DisplayName("삭제 처리된 레슨은 삭제 요청시 조회되지 않은 것으로 처리한다")
	void deleteLesson_AlreadyDeleted() {
		Long lessonId = 1L;
		Long userId = 1L;
		Lesson lesson = createLesson(userId, LocalDateTime.now().plusDays(2));
		lesson.lessonDelete();

		given(userService.getUserById(userId)).willReturn(createUser(userId));
		given(lessonRepository.findById(lessonId)).willReturn(Optional.of(lesson));

		assertThatThrownBy(() -> adminLessonService.deleteLesson(lessonId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LESSON_NOT_FOUND);

		verify(lessonRepository, never()).save(any(Lesson.class));
	}

	@Test
	@DisplayName("모집중이 아닌 레슨 수정시 예외 발생")
	void updateLesson_NotRecruiting() {
		Long lessonId = 1L;
		Long userId = 1L;
		Lesson lesson = createLesson(userId, LocalDateTime.now().plusDays(2));
		lesson.updateStatus(LessonStatus.IN_PROGRESS);

		given(userService.getUserById(userId)).willReturn(createUser(userId));
		given(lessonRepository.findById(lessonId)).willReturn(Optional.of(lesson));

		assertThatThrownBy(() -> adminLessonService.updateLesson(lessonId, basicUpdateRequest(), userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LESSON_NOT_EDITABLE);

		verify(lessonRepository, never()).save(any(Lesson.class));
	}

	@Test
	@DisplayName("수정 가능 시간 제한을 넘긴 레슨 수정시 예외 발생")
	void updateLesson_TimeLimitExceeded() {
		Long lessonId = 1L;
		Long userId = 1L;
		Lesson lesson = createLesson(userId, LocalDateTime.now().plusHours(10));

		given(userService.getUserById(userId)).willReturn(createUser(userId));
		given(lessonRepository.findById(lessonId)).willReturn(Optional.of(lesson));

		assertThatThrownBy(() -> adminLessonService.updateLesson(lessonId, basicUpdateRequest(), userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LESSON_TIME_LIMIT_EXCEEDED);

		verify(lessonRepository, never()).save(any(Lesson.class));
	}

	@Test
	@DisplayName("변경 필드가 없는 레슨 수정 요청시 예외 발생")
	void updateLesson_EmptyRequest() {
		Long lessonId = 1L;
		Long userId = 1L;
		Lesson lesson = createLesson(userId, LocalDateTime.now().plusDays(2));
		LessonUpdateRequestDto request = new LessonUpdateRequestDto(
			null, null, null, null, null, null, null, null, null,
			null, null, null, null, null, null, null, null, null
		);

		given(userService.getUserById(userId)).willReturn(createUser(userId));
		given(lessonRepository.findById(lessonId)).willReturn(Optional.of(lesson));

		assertThatThrownBy(() -> adminLessonService.updateLesson(lessonId, request, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);

		verify(lessonRepository, never()).save(any(Lesson.class));
	}

	@Test
	@DisplayName("승인 참가자가 있는 레슨의 제한 필드 수정시 예외 발생")
	void updateLesson_RestrictedFieldWithParticipants() {
		Long lessonId = 1L;
		Long userId = 1L;
		Lesson lesson = createLesson(userId, LocalDateTime.now().plusDays(2));
		LessonUpdateRequestDto request = new LessonUpdateRequestDto(
			null, null, Category.PILATES, null, null, null, null, null, null,
			null, null, null, null, null, null, null, null, null
		);

		given(userService.getUserById(userId)).willReturn(createUser(userId));
		given(lessonRepository.findById(lessonId)).willReturn(Optional.of(lesson));
		given(lessonApplicationRepository.countByLessonAndStatus(lesson, ApplicationStatus.APPROVED)).willReturn(1);

		assertThatThrownBy(() -> adminLessonService.updateLesson(lessonId, request, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LESSON_PARTICIPANTS_EXIST_RESTRICTION);

		verify(lessonRepository, never()).save(any(Lesson.class));
	}

	@Test
	@DisplayName("레슨 신청 승인 성공")
	void processLessonApplication_Approve_Success() {
		Long applicationId = 1L;
		Long userId = 1L;

		User user = User.builder().id(2L).email("user@test.com").nickname("유저").role(UserRole.USER).build();
		Lesson lesson = Lesson.builder()
			.lessonLeader(userId)
			.lessonName("테스트 레슨")
			.description("테스트 설명")
			.category(Category.GYM)
			.price(30000)
			.maxParticipants(10)
			.startAt(LocalDateTime.now().plusDays(1))
			.endAt(LocalDateTime.now().plusDays(1).plusHours(2))
			.openRun(false)
			.city("서울시")
			.district("강남구")
			.dong("역삼동")
			.addressDetail("테스트 주소")
			.build();

		LessonApplication application = LessonApplication.builder()
			.user(user)
			.lesson(lesson)
			.build();

		given(lessonApplicationRepository.findById(applicationId)).willReturn(Optional.of(application));
		given(lessonApplicationRepository.save(any(LessonApplication.class))).willReturn(application);
		given(lessonParticipantRepository.save(any(LessonParticipant.class))).willReturn(any());

		var response = adminLessonService.processLessonApplication(applicationId, ApplicationAction.APPROVED, userId);

		assertThat(response).isNotNull();
		assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED);
		verify(lessonApplicationRepository).save(application);

		verify(lessonParticipantRepository).save(any(LessonParticipant.class));
	}

	@Test
	@DisplayName("레슨 신청 거절 성공")
	void processLessonApplication_Deny_Success() {
		Long applicationId = 1L;
		Long userId = 1L;
		Lesson lesson = createLesson(userId, LocalDateTime.now().plusDays(1));
		LessonApplication application = LessonApplication.builder()
			.user(createUser(2L))
			.lesson(lesson)
			.build();

		given(lessonApplicationRepository.findById(applicationId)).willReturn(Optional.of(application));
		given(lessonApplicationRepository.save(any(LessonApplication.class))).willAnswer(invocation -> invocation.getArgument(0));

		var response = adminLessonService.processLessonApplication(applicationId, ApplicationAction.DENIED, userId);

		assertThat(response.status()).isEqualTo(ApplicationStatus.DENIED);
		verify(lessonParticipantRepository, never()).save(any(LessonParticipant.class));
		verify(lessonRepository, never()).incrementParticipantCount(anyLong());
	}

	@Test
	@DisplayName("정원이 찬 레슨 신청 승인시 예외 발생")
	void processLessonApplication_ApproveCapacityExceeded() {
		Long applicationId = 1L;
		Long userId = 1L;
		Lesson lesson = createLesson(userId, LocalDateTime.now().plusDays(1));
		ReflectionTestUtils.setField(lesson, "id", 1L);
		ReflectionTestUtils.setField(lesson, "participantCount", 10);
		LessonApplication application = LessonApplication.builder()
			.user(createUser(2L))
			.lesson(lesson)
			.build();

		given(lessonApplicationRepository.findById(applicationId)).willReturn(Optional.of(application));

		assertThatThrownBy(() ->
			adminLessonService.processLessonApplication(applicationId, ApplicationAction.APPROVED, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LESSON_MAX_PARTICIPANTS_EXCEEDED);

		verify(lessonParticipantRepository, never()).save(any(LessonParticipant.class));
		verify(lessonRepository, never()).incrementParticipantCount(anyLong());
		verify(lessonApplicationRepository, never()).save(any(LessonApplication.class));
	}

	@Test
	@DisplayName("이미 처리된 레슨 신청 처리시 예외 발생")
	void processLessonApplication_AlreadyProcessed() {
		Long applicationId = 1L;
		Long userId = 1L;
		LessonApplication application = LessonApplication.builder()
			.user(createUser(2L))
			.lesson(createLesson(userId, LocalDateTime.now().plusDays(1)))
			.build();
		application.approve();

		given(lessonApplicationRepository.findById(applicationId)).willReturn(Optional.of(application));

		assertThatThrownBy(() ->
			adminLessonService.processLessonApplication(applicationId, ApplicationAction.DENIED, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LESSON_APPLICATION_ALREADY_PROCESSED);

		verify(lessonApplicationRepository, never()).save(any(LessonApplication.class));
	}

	@Test
	@DisplayName("다른 강사의 레슨 신청 처리시 예외 발생")
	void processLessonApplication_AccessForbidden() {
		Long applicationId = 1L;
		Long userId = 1L;
		LessonApplication application = LessonApplication.builder()
			.user(createUser(2L))
			.lesson(createLesson(99L, LocalDateTime.now().plusDays(1)))
			.build();

		given(lessonApplicationRepository.findById(applicationId)).willReturn(Optional.of(application));

		assertThatThrownBy(() ->
			adminLessonService.processLessonApplication(applicationId, ApplicationAction.APPROVED, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_FORBIDDEN);

		verify(lessonApplicationRepository, never()).save(any(LessonApplication.class));
	}

	@Test
	@DisplayName("존재하지 않는 레슨 신청 처리시 예외 발생")
	void processLessonApplication_NotFound() {
		Long applicationId = 999L;
		Long userId = 1L;

		given(lessonApplicationRepository.findById(applicationId)).willReturn(Optional.empty());

		assertThatThrownBy(() ->
			adminLessonService.processLessonApplication(applicationId, ApplicationAction.APPROVED, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LESSON_APPLICATION_NOT_FOUND);
	}

	@Test
	@DisplayName("레슨 신청자 전체 목록이 비어 있으면 fetch join 없이 빈 목록과 count를 반환한다")
	void getLessonApplications_AllEmpty() {
		Long lessonId = 1L;
		Long userId = 1L;
		Lesson lesson = createLesson(userId, LocalDateTime.now().plusDays(2));
		ReflectionTestUtils.setField(lesson, "id", lessonId);

		given(userService.getUserById(userId)).willReturn(createUser(userId));
		given(lessonRepository.findById(lessonId)).willReturn(Optional.of(lesson));
		given(lessonApplicationRepository.findIdsByLesson(lessonId, 0, 10)).willReturn(List.of());
		given(lessonApplicationRepository.countAllByLesson(eq(lessonId), anyInt())).willReturn(0);

		LessonApplicationListResponseDto response =
			adminLessonService.getLessonApplications(lessonId, 1, 10, "ALL", userId);

		assertThat(response.lessonApplications()).isEmpty();
		assertThat(response.count()).isZero();
		verify(lessonApplicationRepository, never()).findAllWithUserProfileLesson(anyList());
	}

	@Test
	@DisplayName("잘못된 신청 상태로 신청자 목록을 조회하면 예외 발생")
	void getLessonApplications_InvalidStatus() {
		Long lessonId = 1L;
		Long userId = 1L;
		Lesson lesson = createLesson(userId, LocalDateTime.now().plusDays(2));

		given(userService.getUserById(userId)).willReturn(createUser(userId));
		given(lessonRepository.findById(lessonId)).willReturn(Optional.of(lesson));

		assertThatThrownBy(() -> adminLessonService.getLessonApplications(lessonId, 1, 10, "UNKNOWN", userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_APPLICATION_STATUS);
	}

	@Test
	@DisplayName("레슨 참가자 목록이 비어 있으면 fetch join 없이 빈 목록과 count를 반환한다")
	void getLessonParticipants_Empty() {
		Long lessonId = 1L;
		Long userId = 1L;
		Lesson lesson = createLesson(userId, LocalDateTime.now().plusDays(2));
		ReflectionTestUtils.setField(lesson, "id", lessonId);

		given(userService.getUserById(userId)).willReturn(createUser(userId));
		given(lessonRepository.findById(lessonId)).willReturn(Optional.of(lesson));
		given(lessonParticipantRepository.findIdsByLesson(lessonId, 0, 10)).willReturn(List.of());
		given(lessonParticipantRepository.countAllByLesson(eq(lessonId), anyInt())).willReturn(0);

		ParticipantListResponseDto response = adminLessonService.getLessonParticipants(lessonId, 1, 10, userId);

		assertThat(response.lessonApplications()).isEmpty();
		assertThat(response.count()).isZero();
		verify(lessonParticipantRepository, never()).findAllWithUserAndProfile(anyList());
	}

	@Test
	@DisplayName("강사가 개설한 레슨은 상태 필터로 조회할 수 있다")
	void getCreatedLessons_StatusFilter() {
		Long userId = 1L;
		Lesson lesson = createLesson(userId, LocalDateTime.now().plusDays(2));

		given(userService.getUserById(userId)).willReturn(createUser(userId));
		given(lessonRepository.findCreatedLessonsByStatus(userId, LessonStatus.RECRUITING, 0, 10))
			.willReturn(List.of(lesson));
		given(lessonRepository.countCreatedLessonsByStatus(eq(userId), eq(LessonStatus.RECRUITING), anyInt()))
			.willReturn(1);

		CreatedLessonListResponseDto response =
			adminLessonService.getCreatedLessons(userId, 1, 10, LessonStatus.RECRUITING.name());

		assertThat(response.lessons()).hasSize(1);
		assertThat(response.count()).isEqualTo(1);
		assertThat(response.lessons().get(0).lessonName()).isEqualTo("테스트 레슨");
	}

	@Test
	@DisplayName("잘못된 레슨 상태 필터로 개설 레슨을 조회하면 enum 변환 예외가 발생한다")
	void getCreatedLessons_InvalidStatus() {
		Long userId = 1L;
		given(userService.getUserById(userId)).willReturn(createUser(userId));

		assertThatThrownBy(() -> adminLessonService.getCreatedLessons(userId, 1, 10, "UNKNOWN"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private LessonUpdateRequestDto basicUpdateRequest() {
		return new LessonUpdateRequestDto(
			"수정 레슨",
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null
		);
	}

	private Lesson createLesson(Long leaderId, LocalDateTime startAt) {
		return Lesson.builder()
			.lessonLeader(leaderId)
			.lessonName("테스트 레슨")
			.description("테스트 설명")
			.category(Category.GYM)
			.price(30000)
			.maxParticipants(10)
			.startAt(startAt)
			.endAt(startAt.plusHours(2))
			.openRun(false)
			.city("서울시")
			.district("강남구")
			.dong("역삼동")
			.address("테스트 주소")
			.addressDetail("상세 주소")
			.build();
	}

	private LessonCreateRequestDto createLessonRequest(
		LocalDateTime startAt,
		LocalDateTime endAt,
		boolean openRun,
		int maxParticipants,
		List<String> lessonImages
	) {
		return new LessonCreateRequestDto(
			"테스트 레슨",
			"레슨 설명",
			Category.GYM,
			30000,
			maxParticipants,
			startAt,
			endAt,
			openRun ? startAt.minusHours(1) : null,
			openRun,
			"서울시",
			"강남구",
			"역삼동",
			"",
			"테스트 주소",
			"상세 주소",
			37.5665,
			126.9780,
			lessonImages
		);
	}

	private User createUser(Long userId) {
		return User.builder()
			.id(userId)
			.email("test" + userId + "@test.com")
			.nickname("테스트유저" + userId)
			.role(UserRole.USER)
			.build();
	}
}
