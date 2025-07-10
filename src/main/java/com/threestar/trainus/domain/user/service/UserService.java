package com.threestar.trainus.domain.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	@Transactional
	public SignupResponseDto signup(SignupRequestDto request) {

		if (userRepository.existsByEmail(request.email())) {
			throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
		}

		checkNickname(request.nickname());

		String encodedPassword = passwordEncoder.encode(request.password());

		User newUser = userRepository.save(UserMapper.toEntity(request, encodedPassword));

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

		return UserMapper.toLoginResponseDto(user);
	}

	public void logout(HttpSession session) {
		session.invalidate();
	}

	@Transactional(readOnly = true)
	public void checkNickname(String nickname) {

		if (userRepository.existsByNickname(nickname)) {
			throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
		}
	}
}
