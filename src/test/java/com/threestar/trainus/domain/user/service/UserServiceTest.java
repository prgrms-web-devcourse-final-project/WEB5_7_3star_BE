package com.threestar.trainus.domain.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.threestar.trainus.domain.profile.service.ProfileFacadeService;
import com.threestar.trainus.domain.user.dto.LoginRequestDto;
import com.threestar.trainus.domain.user.dto.LoginResponseDto;
import com.threestar.trainus.domain.user.dto.PasswordUpdateDto;
import com.threestar.trainus.domain.user.dto.SignupRequestDto;
import com.threestar.trainus.domain.user.dto.SignupResponseDto;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import jakarta.servlet.http.HttpSession;

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
	private HttpSession session;

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

			// when
			LoginResponseDto result = userService.login(request, session);

			// then
			assertThat(result).isNotNull();
			assertThat(result.id()).isEqualTo(1L);
			assertThat(result.email()).isEqualTo("test@email.com");
			assertThat(result.nickname()).isEqualTo("testUser");

			verify(session).setAttribute("LOGIN_USER", 1L);
		}

		@Test
		@DisplayName("실패 - 존재하지 않는 이메일")
		void login_fail_userNotFound() {
			// given
			LoginRequestDto request = new LoginRequestDto("test@email.com", "password123");
			given(userRepository.findByEmail(request.email())).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> userService.login(request, session))
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
			assertThatThrownBy(() -> userService.login(request, session))
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
				.isEqualTo(ErrorCode.AUTHENTICATION_REQUIRED);
		}
	}
}