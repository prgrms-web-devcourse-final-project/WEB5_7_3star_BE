package com.threestar.trainus.domain.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.user.dto.SignupRequestDto;
import com.threestar.trainus.domain.user.dto.SignupResponseDto;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.unit.BaseResponse;

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
}
