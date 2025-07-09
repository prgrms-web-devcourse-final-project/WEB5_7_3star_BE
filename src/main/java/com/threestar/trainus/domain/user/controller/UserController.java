package com.threestar.trainus.domain.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.user.dto.LoginRequestDto;
import com.threestar.trainus.domain.user.dto.LoginResponseDto;
import com.threestar.trainus.domain.user.dto.NicknameCheckRequestDto;
import com.threestar.trainus.domain.user.dto.SignupRequestDto;
import com.threestar.trainus.domain.user.dto.SignupResponseDto;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.unit.BaseResponse;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@PostMapping("/signup")
	public ResponseEntity<BaseResponse<SignupResponseDto>> signup(
		@Valid @RequestBody SignupRequestDto request
	) {
		SignupResponseDto response = userService.signup(request);

		return BaseResponse.ok("회원가입이 완료되었습니다.", response, HttpStatus.CREATED);
	}

	@PostMapping("/login")
	public ResponseEntity<BaseResponse<LoginResponseDto>> login(
		@Valid @RequestBody LoginRequestDto request,
		HttpSession session
	) {
		LoginResponseDto response = userService.login(request, session);
		return BaseResponse.ok("로그인이 완료되었습니다", response, HttpStatus.OK);
	}

	@PostMapping("/logout")
	public ResponseEntity<BaseResponse<Void>> logout(HttpSession session) {
		userService.logout(session);
		return BaseResponse.ok("로그아웃이 완료되었습니다.", null, HttpStatus.OK);
	}

	@PostMapping("/verify/check-nickname")
	public ResponseEntity<BaseResponse<Void>> checkNickname(
		@Valid @RequestBody NicknameCheckRequestDto request
	) {
		userService.checkNickname(request.nickname());
		return BaseResponse.ok("사용가능한 닉네임입니다.", null, HttpStatus.OK);
	}
}
