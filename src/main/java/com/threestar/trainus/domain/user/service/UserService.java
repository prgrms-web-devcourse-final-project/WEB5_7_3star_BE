package com.threestar.trainus.domain.user.service;

import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.profile.entity.Profile;
import com.threestar.trainus.domain.profile.repository.ProfileRepository;
import com.threestar.trainus.domain.profile.service.ProfileFacadeService;
import com.threestar.trainus.domain.profile.service.ProfileService;
import com.threestar.trainus.domain.user.dto.LoginRequestDto;
import com.threestar.trainus.domain.user.dto.LoginResponseDto;
import com.threestar.trainus.domain.user.dto.NicknameCheckRequestDto;
import com.threestar.trainus.domain.user.dto.SignupRequestDto;
import com.threestar.trainus.domain.user.dto.SignupResponseDto;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.mapper.UserMapper;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final ProfileFacadeService facadeService;

	@Transactional
	public SignupResponseDto signup(SignupRequestDto request) {

		if (userRepository.existsByEmail(request.email())) {
			throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
		}

		checkNickname(request.nickname());

		String encodedPassword = passwordEncoder.encode(request.password());

		User newUser = userRepository.save(UserMapper.toEntity(request, encodedPassword));

		facadeService.createDefaultProfile(newUser); //기본 프로필 생성.

		return UserMapper.toSignupResponseDto(newUser);
	}

	@Transactional(readOnly = true)
	public LoginResponseDto login(LoginRequestDto request, HttpSession session) {

		User user = userRepository.findByEmail(request.email())
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
		}

		session.setAttribute("LOGIN_USER", user.getId());

		UsernamePasswordAuthenticationToken authToken =
			new UsernamePasswordAuthenticationToken(user.getId(), null, Collections.emptyList());
		SecurityContextHolder.getContext().setAuthentication(authToken);

		return UserMapper.toLoginResponseDto(user);
	}

	public void logout(HttpSession session) {
		session.invalidate();
		SecurityContextHolder.clearContext();
	}

	@Transactional(readOnly = true)
	public void checkNickname(String nickname) {

		if (userRepository.existsByNickname(nickname)) {
			throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
		}
	}
}
