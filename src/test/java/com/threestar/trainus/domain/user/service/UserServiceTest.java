package com.threestar.trainus.domain.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonApplication;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonApplicationRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import com.threestar.trainus.domain.profile.service.ProfileFacadeService;
import com.threestar.trainus.domain.user.dto.LoginRequestDto;
import com.threestar.trainus.domain.user.dto.LoginResponseDto;
import com.threestar.trainus.domain.user.dto.PasswordUpdateDto;
import com.threestar.trainus.domain.user.dto.SignupRequestDto;
import com.threestar.trainus.domain.user.dto.SignupResponseDto;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.config.security.JwtProvider;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

	@InjectMocks
	private UserService userService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ProfileFacadeService facadeService;

	@Mock
	private EmailVerificationService emailVerificationService;

	@Mock
	private JwtProvider jwtProvider;

	@Mock
	private LessonRepository lessonRepository;

	@Mock
	private LessonApplicationRepository lessonApplicationRepository;

	@Nested
	@DisplayName("회원가입")
	class SignupTest {

		@Test
		@DisplayName("성공 - 정상적인 회원가입")
		void signup_success() {
			// given
			SignupRequestDto request = new SignupRequestDto("test@email.com", "password123", "testUser");
			String encodedPassword = "encodedPassword";
			User savedUser = User.builder()
				.id(1L)
				.email("test@email.com")
				.password(encodedPassword)
				.nickname("testUser")
				.role(UserRole.USER)
				.build();

			given(emailVerificationService.isEmailVerified(request.email())).willReturn(true);
			given(userRepository.existsByEmail(request.email())).willReturn(false);
			given(userRepository.existsByNickname(request.nickname())).willReturn(false);
			given(passwordEncoder.encode(request.password())).willReturn(encodedPassword);
			given(userRepository.save(any(User.class))).willReturn(savedUser);

			// when
			SignupResponseDto result = userService.signup(request);

			// then
			assertThat(result).isNotNull();
			assertThat(result.id()).isEqualTo(1L);
			assertThat(result.email()).isEqualTo("test@email.com");
			assertThat(result.nickname()).isEqualTo("testUser");
			assertThat(result.userRole()).isEqualTo(UserRole.USER);

			verify(facadeService).createDefaultProfile(savedUser);
		}

		@Test
		@DisplayName("실패 - 이메일 인증 안됨")
		void signup_fail_emailNotVerified() {
			// given
			SignupRequestDto request = new SignupRequestDto("test@email.com", "password123", "testUser");
			given(emailVerificationService.isEmailVerified(request.email())).willReturn(false);

			// when & then
			assertThatThrownBy(() -> userService.signup(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
		}

		@Test
		@DisplayName("실패 - 이메일 중복")
		void signup_fail_emailExists() {
			// given
			SignupRequestDto request = new SignupRequestDto("test@email.com", "password123", "testUser");
			given(emailVerificationService.isEmailVerified(request.email())).willReturn(true);
			given(userRepository.existsByEmail(request.email())).willReturn(true);

			// when & then
			assertThatThrownBy(() -> userService.signup(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
		}

		@Test
		@DisplayName("실패 - 닉네임 중복")
		void signup_fail_nicknameExists() {
			// given
			SignupRequestDto request = new SignupRequestDto("test@email.com", "password123", "testUser");
			given(emailVerificationService.isEmailVerified(request.email())).willReturn(true);
			given(userRepository.existsByEmail(request.email())).willReturn(false);
			given(userRepository.existsByNickname(request.nickname())).willReturn(true);

			// when & then
			assertThatThrownBy(() -> userService.signup(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.NICKNAME_ALREADY_EXISTS);
		}
	}

	@Nested
	@DisplayName("로그인")
	class LoginTest {

		@Test
		@DisplayName("성공 - 정상적인 로그인")
		void login_success() {
			// given
			LoginRequestDto request = new LoginRequestDto("test@email.com", "password123");
			User user = User.builder()
				.id(1L)
				.email("test@email.com")
				.password("encodedPassword")
				.nickname("testUser")
				.role(UserRole.USER)
				.build();

			given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
			given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
			given(jwtProvider.createAccessToken(user.getId(), user.getRole().name())).willReturn("access-token");
			given(jwtProvider.createRefreshToken(user.getId(), user.getRole().name())).willReturn("refresh-token");

			// when
			LoginResponseDto result = userService.login(request);

			// then
			assertThat(result).isNotNull();
			assertThat(result.id()).isEqualTo(1L);
			assertThat(result.email()).isEqualTo("test@email.com");
			assertThat(result.nickname()).isEqualTo("testUser");
			assertThat(result.accessToken()).isEqualTo("access-token");
			assertThat(result.refreshToken()).isEqualTo("refresh-token");
		}

		@Test
		@DisplayName("실패 - 존재하지 않는 이메일")
		void login_fail_userNotFound() {
			// given
			LoginRequestDto request = new LoginRequestDto("test@email.com", "password123");
			given(userRepository.findByEmail(request.email())).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> userService.login(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_CREDENTIALS);
		}

		@Test
		@DisplayName("실패 - 비밀번호 불일치")
		void login_fail_passwordMismatch() {
			// given
			LoginRequestDto request = new LoginRequestDto("test@email.com", "wrongPassword");
			User user = User.builder()
				.email("test@email.com")
				.password("encodedPassword")
				.build();

			given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
			given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

			// when & then
			assertThatThrownBy(() -> userService.login(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_CREDENTIALS);
		}
	}

	@Nested
	@DisplayName("비밀번호 변경")
	class UpdatePasswordTest {

		@Test
		@DisplayName("성공 - 정상적인 비밀번호 변경")
		void updatePassword_success() {
			// given
			Long userId = 1L;
			PasswordUpdateDto request = new PasswordUpdateDto("currentPwd", "newPwd123", "newPwd123");
			User user = User.builder()
				.id(userId)
				.password("encodedCurrentPwd")
				.build();

			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(passwordEncoder.matches(request.currentPassword(), user.getPassword())).willReturn(true);
			given(passwordEncoder.encode(request.newPassword())).willReturn("encodedNewPwd");

			// when
			userService.updatePassword(request, userId);

			// then
			verify(userRepository).save(user);
			verify(passwordEncoder).encode(request.newPassword());
		}

		@Test
		@DisplayName("실패 - 새 비밀번호 확인 불일치")
		void updatePassword_fail_confirmPasswordMismatch() {
			// given
			Long userId = 1L;
			PasswordUpdateDto request = new PasswordUpdateDto("currentPwd", "newPwd123", "differentPwd");

			// when & then
			assertThatThrownBy(() -> userService.updatePassword(request, userId))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_REQUEST_DATA);
		}

		@Test
		@DisplayName("실패 - 현재 비밀번호 불일치")
		void updatePassword_fail_currentPasswordMismatch() {
			// given
			Long userId = 1L;
			PasswordUpdateDto request = new PasswordUpdateDto("wrongCurrentPwd", "newPwd123", "newPwd123");
			User user = User.builder()
				.id(userId)
				.password("encodedCurrentPwd")
				.build();

			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(passwordEncoder.matches(request.currentPassword(), user.getPassword())).willReturn(false);

			// when & then
			assertThatThrownBy(() -> userService.updatePassword(request, userId))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_REQUEST_DATA);
		}
	}

	@Nested
	@DisplayName("닉네임 중복 체크")
	class CheckNicknameTest {

		@Test
		@DisplayName("성공 - 사용 가능한 닉네임")
		void checkNickname_success() {
			// given
			String nickname = "availableNickname";
			given(userRepository.existsByNickname(nickname)).willReturn(false);

			// when & then
			assertThatNoException().isThrownBy(() -> userService.checkNickname(nickname));
		}

		@Test
		@DisplayName("실패 - 이미 존재하는 닉네임")
		void checkNickname_fail_exists() {
			// given
			String nickname = "existingNickname";
			given(userRepository.existsByNickname(nickname)).willReturn(true);

			// when & then
			assertThatThrownBy(() -> userService.checkNickname(nickname))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.NICKNAME_ALREADY_EXISTS);
		}
	}

	@Nested
	@DisplayName("관리자 권한 검증")
	class AdminValidationTest {

		@Test
		@DisplayName("성공 - 관리자 사용자")
		void validateAdminRole_success() {
			// given
			Long userId = 1L;
			User adminUser = User.builder()
				.id(userId)
				.role(UserRole.ADMIN)
				.build();

			given(userRepository.findById(userId)).willReturn(Optional.of(adminUser));

			// when & then
			assertThatNoException().isThrownBy(() -> userService.validateAdminRole(userId));
		}

		@Test
		@DisplayName("실패 - 일반 사용자")
		void validateAdminRole_fail_notAdmin() {
			// given
			Long userId = 1L;
			User normalUser = User.builder()
				.id(userId)
				.role(UserRole.USER)
				.build();

			given(userRepository.findById(userId)).willReturn(Optional.of(normalUser));

			// when & then
			assertThatThrownBy(() -> userService.validateAdminRole(userId))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ACCESS_FORBIDDEN);
		}

		@Test
		@DisplayName("관리자 사용자를 조회해 반환한다")
		void getAdminUser_success() {
			Long userId = 1L;
			User adminUser = User.builder()
				.id(userId)
				.role(UserRole.ADMIN)
				.build();
			given(userRepository.findById(userId)).willReturn(Optional.of(adminUser));

			User result = userService.getAdminUser(userId);

			assertThat(result).isSameAs(adminUser);
		}

		@Test
		@DisplayName("일반 사용자를 관리자 사용자로 조회하면 예외가 발생한다")
		void getAdminUser_fail_notAdmin() {
			Long userId = 1L;
			User normalUser = User.builder()
				.id(userId)
				.role(UserRole.USER)
				.build();
			given(userRepository.findById(userId)).willReturn(Optional.of(normalUser));

			assertThatThrownBy(() -> userService.getAdminUser(userId))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ACCESS_FORBIDDEN);
		}
	}

	@Nested
	@DisplayName("사용자 존재 검증")
	class ValidateUserExistsTest {

		@Test
		@DisplayName("존재하는 사용자는 예외 없이 통과한다")
		void validateUserExists_success() {
			given(userRepository.existsById(1L)).willReturn(true);

			assertThatNoException().isThrownBy(() -> userService.validateUserExists(1L));
		}

		@Test
		@DisplayName("존재하지 않는 사용자는 USER_NOT_FOUND 예외가 발생한다")
		void validateUserExists_notFound() {
			given(userRepository.existsById(1L)).willReturn(false);

			assertThatThrownBy(() -> userService.validateUserExists(1L))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("회원 탈퇴")
	class WithdrawTest {

		@Test
		@DisplayName("개설 레슨과 활성 신청이 없으면 개인정보를 비식별화하고 탈퇴 처리한다")
		void withdraw_success() {
			Long userId = 1L;
			User user = createUser(userId, UserRole.USER);
			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(lessonRepository.findByLessonLeaderAndDeletedAtIsNull(userId)).willReturn(List.of());
			given(lessonApplicationRepository.findByUserId(userId)).willReturn(List.of());

			userService.withdraw(userId);

			assertThat(user.getDeletedAt()).isNotNull();
			assertThat(user.getNickname()).isEqualTo("탈퇴한사용자" + userId);
			verify(userRepository).save(user);
		}

		@Test
		@DisplayName("강사가 삭제되지 않은 레슨을 가지고 있으면 탈퇴할 수 없다")
		void withdraw_instructorHasLessons() {
			Long userId = 1L;
			User user = createUser(userId, UserRole.USER);
			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(lessonRepository.findByLessonLeaderAndDeletedAtIsNull(userId))
				.willReturn(List.of(createLesson(userId, LocalDateTime.now().plusDays(2))));

			assertThatThrownBy(() -> userService.withdraw(userId))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INSTRUCTOR_HAS_LESSONS);

			verify(userRepository, never()).save(any(User.class));
		}

		@Test
		@DisplayName("미래 시작 레슨 신청이 있으면 탈퇴할 수 없다")
		void withdraw_activeApplications() {
			Long userId = 1L;
			User user = createUser(userId, UserRole.USER);
			LessonApplication application = LessonApplication.builder()
				.user(user)
				.lesson(createLesson(99L, LocalDateTime.now().plusDays(2)))
				.build();
			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(lessonRepository.findByLessonLeaderAndDeletedAtIsNull(userId)).willReturn(List.of());
			given(lessonApplicationRepository.findByUserId(userId)).willReturn(List.of(application));

			assertThatThrownBy(() -> userService.withdraw(userId))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_HAS_ACTIVE_APPLICATIONS);

			verify(userRepository, never()).save(any(User.class));
		}
	}

	private User createUser(Long userId, UserRole role) {
		return User.builder()
			.id(userId)
			.email("test" + userId + "@test.com")
			.password("encoded")
			.nickname("테스트유저" + userId)
			.role(role)
			.build();
	}

	private Lesson createLesson(Long leaderId, LocalDateTime startAt) {
		Lesson lesson = Lesson.builder()
			.lessonLeader(leaderId)
			.lessonName("테스트 레슨")
			.description("레슨 설명")
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
		ReflectionTestUtils.setField(lesson, "id", 1L);
		return lesson;
	}
}
