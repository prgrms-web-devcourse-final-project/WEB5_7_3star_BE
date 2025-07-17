package com.threestar.trainus.domain.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.user.dto.EmailSendRequestDto;
import com.threestar.trainus.domain.user.dto.EmailSendResponseDto;
import com.threestar.trainus.domain.user.dto.EmailVerificationDto;
import com.threestar.trainus.domain.user.dto.LoginRequestDto;
import com.threestar.trainus.domain.user.dto.LoginResponseDto;
import com.threestar.trainus.domain.user.dto.NicknameCheckRequestDto;
import com.threestar.trainus.domain.user.dto.PasswordUpdateDto;
import com.threestar.trainus.domain.user.dto.SignupRequestDto;
import com.threestar.trainus.domain.user.dto.SignupResponseDto;
import com.threestar.trainus.domain.user.dto.UserInfoResponseDto;
import com.threestar.trainus.domain.user.service.EmailVerificationService;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.annotation.LoginUser;
import com.threestar.trainus.global.unit.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "유저 API", description = "유저 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final EmailVerificationService emailVerificationService;

	@PostMapping("/signup")
	@Operation(summary = "회원가입 api")
	public ResponseEntity<BaseResponse<SignupResponseDto>> signup(
		@Valid @RequestBody SignupRequestDto request
	) {
		SignupResponseDto response = userService.signup(request);

		return BaseResponse.ok("회원가입이 완료되었습니다.", response, HttpStatus.CREATED);
	}

	@PostMapping("/login")
	@Operation(summary = "로그인 api")
	public ResponseEntity<BaseResponse<LoginResponseDto>> login(
		@Valid @RequestBody LoginRequestDto request,
		HttpSession session
	) {
		LoginResponseDto response = userService.login(request, session);
		return BaseResponse.ok("로그인이 완료되었습니다", response, HttpStatus.OK);
	}

	@PostMapping("/logout")
	@Operation(summary = "로그아웃 api")
	public ResponseEntity<BaseResponse<Void>> logout(HttpSession session) {
		userService.logout(session);
		return BaseResponse.ok("로그아웃이 완료되었습니다.", null, HttpStatus.OK);
	}

	@PostMapping("/verify/check-nickname")
	@Operation(summary = "닉네임 중복 체크 api")
	public ResponseEntity<BaseResponse<Void>> checkNickname(
		@Valid @RequestBody NicknameCheckRequestDto request
	) {
		userService.checkNickname(request.nickname());
		return BaseResponse.ok("사용가능한 닉네임입니다.", null, HttpStatus.OK);
	}

	@PostMapping("/verify/email-send")
	@Operation(summary = "이메일 인증코드 발송 api", description = "회원가입 중 이메일 인증 코드를 발송")
	public ResponseEntity<BaseResponse<EmailSendResponseDto>> sendVerificationCode(
		@Valid @RequestBody EmailSendRequestDto request
	) {
		EmailSendResponseDto response = emailVerificationService.sendVerificationCode(request);
		return BaseResponse.ok("인증 코드가 이메일로 발송되었습니다.", response, HttpStatus.OK);
	}

	@PostMapping("/verify/email-check")
	@Operation(summary = "이메일 인증코드 인증 api", description = "이메일 인증코드(6자리) 입력 시 인증 가능하고 나머지 회원가입 진행")
	public ResponseEntity<BaseResponse<Void>> confirmVerificationCode(
		@Valid @RequestBody EmailVerificationDto request
	) {
		emailVerificationService.verifyCode(request.email(), request.verificationCode());
		return BaseResponse.ok("이메일 인증이 완료되었습니다.", null, HttpStatus.OK);
	}

	@PatchMapping("/password")
	@Operation(summary = "비밀번호 변경 api")
	public ResponseEntity<BaseResponse<Void>> updatePassword(
		@Valid @RequestBody PasswordUpdateDto request,
		@LoginUser Long loginUserId
	) {
		userService.updatePassword(request, loginUserId);
		return BaseResponse.ok("비밀번호 변경이 완료되었습니다.", null, HttpStatus.OK);
	}

	@GetMapping("/me")
	@Operation(summary = "현재 로그인한 사용자 정보 조회 api")
	public ResponseEntity<BaseResponse<UserInfoResponseDto>> getCurrentUser(
		@LoginUser Long loginUserId
	) {
		UserInfoResponseDto response = userService.getCurrentUserInfo(loginUserId);
		return BaseResponse.ok("사용자 정보 조회가 완료되었습니다.", response, HttpStatus.OK);
	}
}
