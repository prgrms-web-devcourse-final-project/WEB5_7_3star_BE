package com.threestar.trainus.domain.lesson.student.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.threestar.trainus.domain.lesson.issue.LessonApplyProducer;
import com.threestar.trainus.domain.lesson.issue.LessonApplyStreamConstant;
import com.threestar.trainus.domain.lesson.issue.LessonWaitingRoomService;
import com.threestar.trainus.domain.lesson.student.dto.LessonApplyRequestResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonApplyStatusResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonDetailResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonSearchListResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonSimpleResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.MyLessonApplicationListResponseDto;
import com.threestar.trainus.domain.lesson.student.entity.LessonSortType;
import com.threestar.trainus.domain.lesson.teacher.entity.ApplicationStatus;
import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonApplication;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonImage;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonStatus;
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
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

@ExtendWith(MockitoExtension.class)
class StudentLessonServiceTest {

	@Mock
	private LessonRepository lessonRepository;

	@Mock
	private LessonImageRepository lessonImageRepository;

	@Mock
	private ProfileRepository profileRepository;

	@Mock
	private UserService userService;

	@Mock
	private AdminLessonService adminLessonService;

	@Mock
	private ProfileMetadataService profileMetadataService;

	@Mock
	private LessonParticipantRepository lessonParticipantRepository;

	@Mock
	private LessonApplicationRepository lessonApplicationRepository;

	@Mock
	private GeometryFactory geometryFactory;

	@Mock
	private LessonApplyProducer lessonApplyProducer;

	@Mock
	private StringRedisTemplate coreRedisTemplate;

	@Mock
	private StringRedisTemplate mqRedisTemplate;

	@Mock
	private LessonWaitingRoomService waitingRoomService;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private StudentLessonService studentLessonService;

	@BeforeEach
	void setUpRedisTemplates() {
		ReflectionTestUtils.setField(studentLessonService, "coreRedisTemplate", coreRedisTemplate);
		ReflectionTestUtils.setField(studentLessonService, "mqRedisTemplate", mqRedisTemplate);
	}

	@Test
	@DisplayName("수락제 레슨 신청 성공시 PENDING 신청을 저장한다")
	void applyToApprovalLesson_Success() {
		Long lessonId = 1L;
		Long userId = 2L;
		Lesson lesson = createLesson(lessonId, 1L, false);
		User user = createUser(userId);

		given(adminLessonService.findLessonById(lessonId)).willReturn(lesson);
		given(userService.getUserById(userId)).willReturn(user);
		given(lessonParticipantRepository.existsByLessonIdAndUserId(lessonId, userId)).willReturn(false);
		given(lessonApplicationRepository.existsByLessonIdAndUserId(lessonId, userId)).willReturn(false);
		given(lessonApplicationRepository.save(any(LessonApplication.class))).willAnswer(invocation -> invocation.getArgument(0));

		LessonApplyRequestResponseDto response = studentLessonService.applyToApprovalLesson(lessonId, userId);

		assertThat(response.lessonId()).isEqualTo(lessonId);
		assertThat(response.userId()).isEqualTo(userId);
		assertThat(response.status()).isEqualTo(ApplicationStatus.PENDING.name());

		ArgumentCaptor<LessonApplication> captor = ArgumentCaptor.forClass(LessonApplication.class);
		verify(lessonApplicationRepository).save(captor.capture());
		assertThat(captor.getValue().getLesson()).isSameAs(lesson);
		assertThat(captor.getValue().getUser()).isSameAs(user);
		assertThat(captor.getValue().getStatus()).isEqualTo(ApplicationStatus.PENDING);
	}

	@Test
	@DisplayName("레슨 개설자는 수락제 레슨에 신청할 수 없다")
	void applyToApprovalLesson_CreatorCannotApply() {
		Long lessonId = 1L;
		Long userId = 1L;

		given(adminLessonService.findLessonById(lessonId)).willReturn(createLesson(lessonId, userId, false));
		given(userService.getUserById(userId)).willReturn(createUser(userId));

		assertThatThrownBy(() -> studentLessonService.applyToApprovalLesson(lessonId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LESSON_CREATOR_CANNOT_APPLY);

		verify(lessonApplicationRepository, never()).save(any(LessonApplication.class));
	}

	@Test
	@DisplayName("이미 참가한 사용자는 수락제 레슨에 중복 신청할 수 없다")
	void applyToApprovalLesson_AlreadyParticipated() {
		Long lessonId = 1L;
		Long userId = 2L;

		given(adminLessonService.findLessonById(lessonId)).willReturn(createLesson(lessonId, 1L, false));
		given(userService.getUserById(userId)).willReturn(createUser(userId));
		given(lessonParticipantRepository.existsByLessonIdAndUserId(lessonId, userId)).willReturn(true);

		assertThatThrownBy(() -> studentLessonService.applyToApprovalLesson(lessonId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_APPLIED);

		verify(lessonApplicationRepository, never()).save(any(LessonApplication.class));
	}

	@Test
	@DisplayName("이미 신청한 사용자는 수락제 레슨에 중복 신청할 수 없다")
	void applyToApprovalLesson_AlreadyApplied() {
		Long lessonId = 1L;
		Long userId = 2L;

		given(adminLessonService.findLessonById(lessonId)).willReturn(createLesson(lessonId, 1L, false));
		given(userService.getUserById(userId)).willReturn(createUser(userId));
		given(lessonParticipantRepository.existsByLessonIdAndUserId(lessonId, userId)).willReturn(false);
		given(lessonApplicationRepository.existsByLessonIdAndUserId(lessonId, userId)).willReturn(true);

		assertThatThrownBy(() -> studentLessonService.applyToApprovalLesson(lessonId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_APPLIED);

		verify(lessonApplicationRepository, never()).save(any(LessonApplication.class));
	}

	@Test
	@DisplayName("모집중이 아닌 레슨에는 수락제 신청할 수 없다")
	void applyToApprovalLesson_NotRecruiting() {
		Long lessonId = 1L;
		Long userId = 2L;
		Lesson lesson = createLesson(lessonId, 1L, false);
		lesson.updateStatus(LessonStatus.RECRUITMENT_COMPLETED);

		given(adminLessonService.findLessonById(lessonId)).willReturn(lesson);
		given(userService.getUserById(userId)).willReturn(createUser(userId));

		assertThatThrownBy(() -> studentLessonService.applyToApprovalLesson(lessonId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LESSON_NOT_AVAILABLE);

		verify(lessonApplicationRepository, never()).save(any(LessonApplication.class));
	}

	@Test
	@DisplayName("선착순 레슨은 수락제 신청 경로로 신청할 수 없다")
	void applyToApprovalLesson_OpenRun() {
		Long lessonId = 1L;
		Long userId = 2L;

		given(adminLessonService.findLessonById(lessonId)).willReturn(createLesson(lessonId, 1L, true));
		given(userService.getUserById(userId)).willReturn(createUser(userId));

		assertThatThrownBy(() -> studentLessonService.applyToApprovalLesson(lessonId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LESSON_NOT_AVAILABLE);

		verify(lessonApplicationRepository, never()).save(any(LessonApplication.class));
	}

	@Test
	@DisplayName("PENDING 수락제 신청 취소 성공시 신청을 삭제한다")
	void cancelLessonApplication_Success() {
		Long lessonId = 1L;
		Long userId = 2L;
		LessonApplication application = LessonApplication.builder()
			.lesson(createLesson(lessonId, 1L, false))
			.user(createUser(userId))
			.build();

		given(adminLessonService.findLessonById(lessonId)).willReturn(createLesson(lessonId, 1L, false));
		willDoNothing().given(userService).validateUserExists(userId);
		given(lessonApplicationRepository.findByLessonIdAndUserId(lessonId, userId)).willReturn(Optional.of(application));

		studentLessonService.cancelLessonApplication(lessonId, userId);

		verify(lessonApplicationRepository).delete(application);
	}

	@Test
	@DisplayName("선착순 레슨 신청은 수락제 신청 취소 경로로 취소할 수 없다")
	void cancelLessonApplication_OpenRun() {
		Long lessonId = 1L;
		Long userId = 2L;

		given(adminLessonService.findLessonById(lessonId)).willReturn(createLesson(lessonId, 1L, true));
		willDoNothing().given(userService).validateUserExists(userId);

		assertThatThrownBy(() -> studentLessonService.cancelLessonApplication(lessonId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_CANCEL_APPROVED_APPLICATION);

		verify(lessonApplicationRepository, never()).delete(any(LessonApplication.class));
	}

	@Test
	@DisplayName("신청 내역이 없으면 수락제 신청 취소시 예외 발생")
	void cancelLessonApplication_NotFound() {
		Long lessonId = 1L;
		Long userId = 2L;

		given(adminLessonService.findLessonById(lessonId)).willReturn(createLesson(lessonId, 1L, false));
		willDoNothing().given(userService).validateUserExists(userId);
		given(lessonApplicationRepository.findByLessonIdAndUserId(lessonId, userId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> studentLessonService.cancelLessonApplication(lessonId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LESSON_APPLICATION_NOT_FOUND);

		verify(lessonApplicationRepository, never()).delete(any(LessonApplication.class));
	}

	@Test
	@DisplayName("PENDING이 아닌 신청은 수락제 신청 취소시 예외 발생")
	void cancelLessonApplication_NotPending() {
		Long lessonId = 1L;
		Long userId = 2L;
		LessonApplication application = LessonApplication.builder()
			.lesson(createLesson(lessonId, 1L, false))
			.user(createUser(userId))
			.build();
		application.approve();

		given(adminLessonService.findLessonById(lessonId)).willReturn(createLesson(lessonId, 1L, false));
		willDoNothing().given(userService).validateUserExists(userId);
		given(lessonApplicationRepository.findByLessonIdAndUserId(lessonId, userId)).willReturn(Optional.of(application));

		assertThatThrownBy(() -> studentLessonService.cancelLessonApplication(lessonId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_CANCEL_APPROVED_APPLICATION);

		verify(lessonApplicationRepository, never()).delete(any(LessonApplication.class));
	}

	@Test
	@DisplayName("선착순 레슨 신청은 requestId와 WAITING 상태를 반환한다")
	void applyToOpenRunLesson_Success() {
		Long lessonId = 1L;
		Long userId = 2L;
		given(lessonApplyProducer.send(lessonId, userId)).willReturn("request-1");

		LessonApplyRequestResponseDto response = studentLessonService.applyToOpenRunLesson(lessonId, userId);

		assertThat(response.lessonId()).isEqualTo(lessonId);
		assertThat(response.userId()).isEqualTo(userId);
		assertThat(response.requestId()).isEqualTo("request-1");
		assertThat(response.status()).isEqualTo(LessonApplyStreamConstant.STATUS_WAITING);
	}

	@Test
	@DisplayName("선착순 레슨 신청 큐가 중복을 반환하면 중복 신청 예외가 발생한다")
	void applyToOpenRunLesson_AlreadyApplied() {
		Long lessonId = 1L;
		Long userId = 2L;
		given(lessonApplyProducer.send(lessonId, userId)).willReturn("ALREADY_APPLIED");

		assertThatThrownBy(() -> studentLessonService.applyToOpenRunLesson(lessonId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_APPLIED);
	}

	@Test
	@DisplayName("선착순 레슨 신청 큐가 요청을 만들지 못하면 신청 불가 예외가 발생한다")
	void applyToOpenRunLesson_NotAvailable() {
		Long lessonId = 1L;
		Long userId = 2L;
		given(lessonApplyProducer.send(lessonId, userId)).willReturn(null);

		assertThatThrownBy(() -> studentLessonService.applyToOpenRunLesson(lessonId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.LESSON_NOT_AVAILABLE);
	}

	@Test
	@DisplayName("레슨 상세 조회는 리더 프로필, 메타데이터, 이미지 URL을 포함한다")
	void getLessonDetail_Success() {
		Long lessonId = 1L;
		Long leaderId = 10L;
		Lesson lesson = createLesson(lessonId, leaderId, false);
		User leader = createUser(leaderId);
		Profile profile = createProfile(leader, "https://cdn.test/profile.png", "소개");
		ProfileMetadataResponseDto metadata = new ProfileMetadataResponseDto(leaderId, 7, 4.5);

		given(adminLessonService.findLessonById(lessonId)).willReturn(lesson);
		given(userService.getUserById(leaderId)).willReturn(leader);
		given(profileRepository.findByUserId(leaderId)).willReturn(Optional.of(profile));
		given(profileMetadataService.getMetadata(leaderId)).willReturn(metadata);
		given(lessonImageRepository.findAllByLessonId(lessonId)).willReturn(List.of(
			LessonImage.builder().lesson(lesson).imageUrl("https://cdn.test/lesson.png").build()
		));

		LessonDetailResponseDto response = studentLessonService.getLessonDetail(lessonId);

		assertThat(response.id()).isEqualTo(lessonId);
		assertThat(response.lessonLeader()).isEqualTo(leaderId);
		assertThat(response.lessonLeaderName()).isEqualTo(leader.getNickname());
		assertThat(response.profileImage()).isEqualTo("https://cdn.test/profile.png");
		assertThat(response.reviewCount()).isEqualTo(7);
		assertThat(response.rating()).isEqualTo(4.5);
		assertThat(response.lessonImages()).containsExactly("https://cdn.test/lesson.png");
	}

	@Test
	@DisplayName("레슨 상세 조회시 리더 프로필이 없으면 예외 발생")
	void getLessonDetail_ProfileNotFound() {
		Long lessonId = 1L;
		Long leaderId = 10L;
		given(adminLessonService.findLessonById(lessonId)).willReturn(createLesson(lessonId, leaderId, false));
		given(userService.getUserById(leaderId)).willReturn(createUser(leaderId));
		given(profileRepository.findByUserId(leaderId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> studentLessonService.getLessonDetail(lessonId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROFILE_NOT_FOUND);
	}

	@Test
	@DisplayName("내 레슨 신청 목록이 비어 있으면 fetch join 없이 빈 목록과 count를 반환한다")
	void getMyLessonApplications_AllEmpty() {
		Long userId = 2L;
		given(lessonApplicationRepository.findIdsByUserAndStatus(userId, null, 0, 10)).willReturn(List.of());
		given(lessonApplicationRepository.countByUserAndStatus(eq(userId), isNull(), anyInt())).willReturn(0);

		MyLessonApplicationListResponseDto response =
			studentLessonService.getMyLessonApplications(userId, 1, 10, "ALL");

		assertThat(response.lessonApplications()).isEmpty();
		assertThat(response.count()).isZero();
		verify(lessonApplicationRepository, never()).findAllWithFetchJoin(anyList());
	}

	@Test
	@DisplayName("잘못된 신청 상태로 내 신청 목록을 조회하면 예외 발생")
	void getMyLessonApplications_InvalidStatus() {
		assertThatThrownBy(() -> studentLessonService.getMyLessonApplications(2L, 1, 10, "UNKNOWN"))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_APPLICATION_STATUS);
	}

	@Test
	@DisplayName("간단 레슨 조회는 핵심 레슨 정보를 반환한다")
	void getLessonSimple_Success() {
		Long lessonId = 1L;
		Lesson lesson = createLesson(lessonId, 10L, false);
		given(adminLessonService.findLessonById(lessonId)).willReturn(lesson);

		LessonSimpleResponseDto response = studentLessonService.getLessonSimple(lessonId);

		assertThat(response.lessonId()).isEqualTo(lessonId);
		assertThat(response.lessonName()).isEqualTo("테스트 레슨");
		assertThat(response.price()).isEqualTo(30000L);
		assertThat(response.addressDetail()).isEqualTo("상세 주소");
	}

	@Test
	@DisplayName("검색 정렬 기준이 없으면 주소 기반 검색 전에 예외 발생")
	void searchLessons_InvalidSort() {
		assertThatThrownBy(() ->
			studentLessonService.searchLessons(1, 10, Category.GYM, "", "서울시", "강남구", "역삼동", "", null))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SORT);

		verify(lessonRepository, never()).findLessonsWithoutFullText(any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
	}

	@Test
	@DisplayName("검색어가 없으면 일반 주소 기반 검색과 count를 사용한다")
	void searchLessons_WithoutKeywordEmpty() {
		given(lessonRepository.findLessonsWithoutFullText(
			Category.GYM.name(), "서울시", "강남구", "역삼동", "", LessonSortType.LATEST.name(), 0, 10
		)).willReturn(List.of());
		given(lessonRepository.countLessonsWithoutFullText(eq(Category.GYM.name()), eq("서울시"), eq("강남구"), eq("역삼동"), eq(""), anyInt()))
			.willReturn(0);

		LessonSearchListResponseDto response =
			studentLessonService.searchLessons(1, 10, Category.GYM, "", "서울시", "강남구", "역삼동", "", LessonSortType.LATEST);

		assertThat(response.lessons()).isEmpty();
		assertThat(response.count()).isZero();
		verify(lessonRepository, never()).findLessonsWithFullText(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
	}

	@Test
	@DisplayName("검색어가 있으면 full-text 검색과 page 기반 offset을 사용한다")
	void searchLessons_WithKeywordUsesFullTextAndOffset() {
		Lesson lesson = createLesson(1L, 10L, false);
		User leader = createUser(10L);
		Profile profile = createProfile(leader, "https://cdn.test/profile.png", "소개");
		ProfileMetadataResponseDto metadata = new ProfileMetadataResponseDto(10L, 3, 4.0);
		given(lessonRepository.findLessonsWithFullText(
			Category.GYM.name(), "서울시", "강남구", "역삼동", "", "요가", LessonSortType.PRICE_LOW.name(), 10, 10
			)).willReturn(List.of(lesson));
			given(lessonRepository.countLessonsWithFullText(
				eq(Category.GYM.name()), eq("서울시"), eq("강남구"), eq("역삼동"), eq(""), eq("요가"), eq(51)
			)).willReturn(1);
		given(userService.getUserById(10L)).willReturn(leader);
		given(profileRepository.findByUserId(10L)).willReturn(Optional.of(profile));
		given(profileMetadataService.getMetadata(10L)).willReturn(metadata);
		given(lessonImageRepository.findAllByLessonId(1L)).willReturn(List.of(
			LessonImage.builder().lesson(lesson).imageUrl("https://cdn.test/lesson.png").build()
		));

		LessonSearchListResponseDto response =
			studentLessonService.searchLessons(2, 10, Category.GYM, "요가", "서울시", "강남구", "역삼동", "", LessonSortType.PRICE_LOW);

		assertThat(response.count()).isEqualTo(1);
		assertThat(response.lessons()).hasSize(1);
		assertThat(response.lessons().get(0).lessonName()).isEqualTo("테스트 레슨");
		assertThat(response.lessons().get(0).lessonLeaderName()).isEqualTo(leader.getNickname());
		assertThat(response.lessons().get(0).lessonImages()).containsExactly("https://cdn.test/lesson.png");
		verify(lessonRepository, never()).findLessonsWithoutFullText(any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
	}

	@Test
	@DisplayName("위치 기반 검색 정렬 기준이 없으면 공간 쿼리 전에 예외 발생")
	void searchLessonsByLocation_InvalidSort() {
		assertThatThrownBy(() ->
			studentLessonService.searchLessonsByLocation(1, 10, 1000, Category.GYM, "", 37.5665, 126.9780, null))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SORT);

		verify(geometryFactory, never()).createPoint(any(Coordinate.class));
	}

	@Test
	@DisplayName("검색어가 없으면 위치 기반 검색과 count를 사용한다")
	void searchLessonsByLocation_WithoutKeywordEmpty() {
		Point point = new GeometryFactory().createPoint(new Coordinate(126.9780, 37.5665));
		given(geometryFactory.createPoint(any(Coordinate.class))).willReturn(point);
		given(lessonRepository.findLessonsByLocationWithoutKeyword(
			Category.GYM.name(), point, 1000, LessonSortType.LATEST.name(), 0, 10
		)).willReturn(List.of());
		given(lessonRepository.countLessonsByLocationWithoutKeyword(eq(Category.GYM.name()), eq(point), eq(1000), anyInt()))
			.willReturn(0);

		LessonSearchListResponseDto response =
			studentLessonService.searchLessonsByLocation(1, 10, 1000, Category.GYM, "", 37.5665, 126.9780, LessonSortType.LATEST);

		assertThat(response.lessons()).isEmpty();
		assertThat(response.count()).isZero();
		verify(lessonRepository, never()).findLessonsByLocationWithKeyword(any(), any(), anyInt(), any(), any(), anyInt(), anyInt());
	}

	@Test
	@DisplayName("비동기 신청 상태가 없으면 예외 발생")
	void getAsyncApplyStatus_NotFound() {
		String requestId = "request-1";
		given(mqRedisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(LessonApplyStreamConstant.STATUS_PREFIX + requestId)).willReturn(null);

		assertThatThrownBy(() -> studentLessonService.getAsyncApplyStatus(requestId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.REQUEST_NOT_FOUND);
	}

	@Test
	@DisplayName("WAITING 상태는 대기열 순번을 포함해 반환한다")
	void getAsyncApplyStatus_WaitingWithRank() {
		String requestId = "request-1";
		String status = LessonApplyStreamConstant.STATUS_WAITING + ":1:2";
		given(mqRedisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(LessonApplyStreamConstant.STATUS_PREFIX + requestId)).willReturn(status);
		given(waitingRoomService.getRank(1L, requestId)).willReturn(Optional.of(3L));

		LessonApplyStatusResponseDto response = studentLessonService.getAsyncApplyStatus(requestId);

		assertThat(response.getStatus()).isEqualTo(LessonApplyStreamConstant.STATUS_WAITING);
		assertThat(response.getRank()).isEqualTo(3L);
		assertThat(response.getEstimatedWaitTimeMs()).isEqualTo(300L);
	}

	@Test
	@DisplayName("WAITING 상태지만 대기열에 없으면 처리중으로 반환한다")
	void getAsyncApplyStatus_WaitingWithoutRank() {
		String requestId = "request-1";
		String status = LessonApplyStreamConstant.STATUS_WAITING + ":1:2";
		given(mqRedisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(LessonApplyStreamConstant.STATUS_PREFIX + requestId)).willReturn(status);
		given(waitingRoomService.getRank(1L, requestId)).willReturn(Optional.empty());

		LessonApplyStatusResponseDto response = studentLessonService.getAsyncApplyStatus(requestId);

		assertThat(response.getStatus()).isEqualTo(LessonApplyStreamConstant.STATUS_PROCESSING);
		assertThat(response.getRank()).isNull();
	}

	@Test
	@DisplayName("PROCESSING 상태는 그대로 반환한다")
	void getAsyncApplyStatus_Processing() {
		String requestId = "request-1";
		given(mqRedisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(LessonApplyStreamConstant.STATUS_PREFIX + requestId))
			.willReturn(LessonApplyStreamConstant.STATUS_PROCESSING);

		LessonApplyStatusResponseDto response = studentLessonService.getAsyncApplyStatus(requestId);

		assertThat(response.getStatus()).isEqualTo(LessonApplyStreamConstant.STATUS_PROCESSING);
		assertThat(response.getRank()).isNull();
	}

	@Test
	@DisplayName("SUCCESS 상태는 그대로 반환한다")
	void getAsyncApplyStatus_Success() {
		String requestId = "request-1";
		given(mqRedisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(LessonApplyStreamConstant.STATUS_PREFIX + requestId))
			.willReturn(LessonApplyStreamConstant.STATUS_SUCCESS);

		LessonApplyStatusResponseDto response = studentLessonService.getAsyncApplyStatus(requestId);

		assertThat(response.getStatus()).isEqualTo(LessonApplyStreamConstant.STATUS_SUCCESS);
		assertThat(response.getRank()).isNull();
	}

	private Lesson createLesson(Long lessonId, Long leaderId, boolean openRun) {
		LocalDateTime startAt = LocalDateTime.now().plusDays(2);
		Lesson lesson = Lesson.builder()
			.lessonLeader(leaderId)
			.lessonName("테스트 레슨")
			.description("테스트 설명")
			.category(Category.GYM)
			.price(30000)
			.maxParticipants(10)
			.startAt(startAt)
			.endAt(startAt.plusHours(2))
			.openTime(startAt.minusHours(1))
			.openRun(openRun)
			.city("서울시")
			.district("강남구")
			.dong("역삼동")
			.address("테스트 주소")
			.addressDetail("상세 주소")
			.build();
		ReflectionTestUtils.setField(lesson, "id", lessonId);
		return lesson;
	}

	private User createUser(Long userId) {
		return User.builder()
			.id(userId)
			.email("test" + userId + "@test.com")
			.nickname("테스트유저" + userId)
			.role(UserRole.USER)
			.build();
	}

	private Profile createProfile(User user, String profileImage, String intro) {
		return Profile.builder()
			.user(user)
			.profileImage(profileImage)
			.intro(intro)
			.build();
	}
}
