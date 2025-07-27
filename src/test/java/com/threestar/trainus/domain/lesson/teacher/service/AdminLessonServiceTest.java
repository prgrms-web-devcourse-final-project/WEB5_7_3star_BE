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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threestar.trainus.domain.lesson.teacher.dto.LessonCreateRequestDto;
import com.threestar.trainus.domain.lesson.teacher.dto.LessonResponseDto;
import com.threestar.trainus.domain.lesson.teacher.entity.ApplicationAction;
import com.threestar.trainus.domain.lesson.teacher.entity.ApplicationStatus;
import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonApplication;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonApplicationRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonImageRepository;
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
			"테스트 주소",
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

		LessonResponseDto response = adminLessonService.createLesson(request, userId);

		assertThat(response).isNotNull();
		assertThat(response.lessonName()).isEqualTo("테스트 레슨");
		assertThat(response.lessonLeader()).isEqualTo(userId);
		verify(lessonRepository).save(any(Lesson.class));
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
			"테스트 주소",
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
			"테스트 주소",
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

		var response = adminLessonService.processLessonApplication(applicationId, ApplicationAction.APPROVED, userId);

		assertThat(response).isNotNull();
		assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED);
		verify(lessonApplicationRepository).save(application);
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
}
