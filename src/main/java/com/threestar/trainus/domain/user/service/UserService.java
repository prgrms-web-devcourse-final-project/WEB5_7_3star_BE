package com.threestar.trainus.domain.user.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.threestar.trainus.domain.user.dto.UserInfoResponseDto;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.mapper.UserMapper;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.config.security.JwtProvider;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final ProfileFacadeService facadeService;
	private final EmailVerificationService emailVerificationService;
	private final LessonRepository lessonRepository;
	private final LessonApplicationRepository lessonApplicationRepository;
	private final JwtProvider jwtProvider;

	@Transactional
	public SignupResponseDto signup(SignupRequestDto request) {
		if (!emailVerificationService.isEmailVerified(request.email())) {
			throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
		}
		if (userRepository.existsByEmail(request.email())) {
			throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
		}
		if (userRepository.existsByNickname(request.nickname())) {
			throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
		}
		String encodedPassword = passwordEncoder.encode(request.password());
		User newUser = userRepository.save(UserMapper.toEntity(request, encodedPassword));
		facadeService.createDefaultProfile(newUser);
		return UserMapper.toSignupResponseDto(newUser);
	}

	@Transactional(readOnly = true)
	public LoginResponseDto login(LoginRequestDto request) {
		User user = userRepository.findByEmail(request.email())
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
		}

		String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().name());
		String refreshToken = jwtProvider.createRefreshToken(user.getId(), user.getRole().name());

		log.info("[JWT] Token issued for user: {}", user.getEmail());

		return UserMapper.toLoginResponseDto(user, accessToken, refreshToken);
	}

	public void logout() {
		SecurityContextHolder.clearContext();
	}

	@Transactional(readOnly = true)
	public void checkNickname(String nickname) {
		if (userRepository.existsByNickname(nickname)) {
			throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
		}
	}

	@Transactional(readOnly = true)
	public User getUserById(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public void validateUserExists(Long userId) {
		if (!userRepository.existsById(userId)) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
	}

	@Transactional(readOnly = true)
	public void validateAdminRole(Long userId) {
		User user = getUserById(userId);
		if (user.getRole() != UserRole.ADMIN) {
			throw new BusinessException(ErrorCode.ACCESS_FORBIDDEN);
		}
	}

	@Transactional(readOnly = true)
	public User getAdminUser(Long userId) {
		User user = getUserById(userId);
		if (user.getRole() != UserRole.ADMIN) {
			throw new BusinessException(ErrorCode.ACCESS_FORBIDDEN);
		}
		return user;
	}

	@Transactional
	public void updatePassword(PasswordUpdateDto request, Long userId) {
		//새 비밀번호와 새 비밀번호 확인끼리의 검증
		if (!request.newPassword().equals(request.confirmPassword())) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
		}
		User user = getUserById(userId);
		//유저의 현재 비밀번호 검증
		if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
		}
		String encodedNewPassword = passwordEncoder.encode(request.newPassword());
		user.updatePassword(encodedNewPassword);
		userRepository.save(user);
	}

	@Transactional
	public void withdraw(Long userId) {
		User user = getUserById(userId);

		//강사는 탈퇴 불가 (레슨 있을 시)
		validateInstructorCanWithdraw(user);

		//사용자의 활성 레슨 신청이 있으면 탈퇴 불가
		validateUserHasNoActiveApplications(user);

		//개인정보 비식별화
		String anonymizedNickname = "탈퇴한사용자" + user.getId();
		user.anonymizePersonalData(anonymizedNickname);
		user.withdraw();
		userRepository.save(user);

		SecurityContextHolder.clearContext();
	}

	private void validateInstructorCanWithdraw(User user) {
		List<Lesson> instructorLessons = lessonRepository.findByLessonLeaderAndDeletedAtIsNull(user.getId());
		if (!instructorLessons.isEmpty()) {
			throw new BusinessException(ErrorCode.INSTRUCTOR_HAS_LESSONS);
		}
	}

	private void validateUserHasNoActiveApplications(User user) {
		List<LessonApplication> applications = lessonApplicationRepository.findByUserId(user.getId());
		if (applications.isEmpty()) {
			return;
		}
		LocalDateTime now = LocalDateTime.now();
		long activeApplicationsCount = applications.stream()
			.filter(application -> application.getLesson().getStartAt().isAfter(now))
			.count();
		if (activeApplicationsCount > 0) {
			throw new BusinessException(ErrorCode.USER_HAS_ACTIVE_APPLICATIONS);
		}
	}

	@Transactional(readOnly = true)
	public UserInfoResponseDto getCurrentUserInfo(Long userId) {
		User user = getUserById(userId);
		return UserMapper.toUserInfoResponseDto(user);
	}
}
