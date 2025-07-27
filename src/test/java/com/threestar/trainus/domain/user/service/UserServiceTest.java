package com.threestar.trainus.domain.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.threestar.trainus.domain.profile.service.ProfileFacadeService;
import com.threestar.trainus.domain.user.dto.PasswordUpdateDto;
import com.threestar.trainus.domain.user.dto.SignupRequestDto;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ProfileFacadeService facadeService;

	@Mock
	private EmailVerificationService emailVerificationService;

	@InjectMocks
	private UserService userService;

	@Test
	@DisplayName("회원가입 시 이메일이 인증되지 않으면 예외가 발생한다")
	void signup_emailNotVerified_shouldThrowException() {
		// given
		SignupRequestDto request = new SignupRequestDto(
			"test@example.com", "password", "nickname"
		);

		given(emailVerificationService.isEmailVerified(request.email())).willReturn(false);

		// when & then
		assertThatThrownBy(() -> userService.signup(request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_NOT_VERIFIED);
	}

	@Test
	@DisplayName("회원가입 시 이메일이 중복되면 예외가 발생한다")
	void signup_duplicateEmail_shouldThrowException() {
		// given
		SignupRequestDto request = new SignupRequestDto(
			"test@example.com", "password", "nickname"
		);

		given(emailVerificationService.isEmailVerified(request.email())).willReturn(true);
		given(userRepository.existsByEmail(request.email())).willReturn(true);

		// when & then
		assertThatThrownBy(() -> userService.signup(request))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);
	}

	@Test
	@DisplayName("비밀번호 변경 시 새 비밀번호와 확인 비밀번호가 다르면 예외가 발생한다")
	void updatePassword_passwordMismatch_shouldThrowException() {
		// given
		PasswordUpdateDto request = new PasswordUpdateDto(
			"currentPassword", "newPassword", "differentPassword"
		);
		Long userId = 1L;

		// when & then
		assertThatThrownBy(() -> userService.updatePassword(request, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
	}

	@Test
	@DisplayName("비밀번호 변경 시 현재 비밀번호가 틀리면 예외가 발생한다")
	void updatePassword_wrongCurrentPassword_shouldThrowException() {
		// given
		PasswordUpdateDto request = new PasswordUpdateDto(
			"wrongPassword", "newPassword", "newPassword"
		);
		Long userId = 1L;

		User user = User.builder()
			.id(userId)
			.email("test@example.com")
			.password("encodedCurrentPassword")
			.nickname("testUser")
			.role(UserRole.USER)
			.build();

		given(userRepository.findById(userId)).willReturn(Optional.of(user));
		given(passwordEncoder.matches(request.currentPassword(), user.getPassword())).willReturn(false);

		// when & then
		assertThatThrownBy(() -> userService.updatePassword(request, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
	}

	@Test
	@DisplayName("사용자 탈퇴가 정상적으로 처리된다")
	void withdraw_shouldSetDeletedAt() {
		// given
		Long userId = 1L;
		User user = User.builder()
			.id(userId)
			.email("test@example.com")
			.password("password")
			.nickname("testUser")
			.role(UserRole.USER)
			.build();

		given(userRepository.findById(userId)).willReturn(Optional.of(user));

		// when
		userService.withdraw(userId);

		// then
		assertThat(user.getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("존재하지 않는 사용자 조회 시 예외가 발생한다")
	void getUserById_userNotFound_shouldThrowException() {
		// given
		Long userId = 999L;
		given(userRepository.findById(userId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userService.getUserById(userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
	}

	@Test
	@DisplayName("일반 사용자가 관리자 권한 검증 시 예외가 발생한다")
	void validateAdminRole_notAdmin_shouldThrowException() {
		// given
		Long userId = 1L;
		User user = User.builder()
			.id(userId)
			.email("test@example.com")
			.password("password")
			.nickname("testUser")
			.role(UserRole.USER)
			.build();

		given(userRepository.findById(userId)).willReturn(Optional.of(user));

		// when & then
		assertThatThrownBy(() -> userService.validateAdminRole(userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTHENTICATION_REQUIRED);
	}
}