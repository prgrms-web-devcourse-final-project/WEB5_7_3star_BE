package com.threestar.trainus.domain.user.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.user.dto.EmailVerificationDto;
import com.threestar.trainus.domain.user.dto.EmailSendRequestDto;
import com.threestar.trainus.domain.user.dto.EmailSendResponseDto;
import com.threestar.trainus.domain.user.dto.LoginRequestDto;
import com.threestar.trainus.domain.user.dto.LoginResponseDto;
import com.threestar.trainus.domain.user.dto.NicknameCheckRequestDto;
import com.threestar.trainus.domain.user.dto.SignupRequestDto;
import com.threestar.trainus.domain.user.dto.SignupResponseDto;
import com.threestar.trainus.domain.user.service.EmailVerificationService;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.unit.BaseResponse;

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

	@PostMapping("/verify/email-send")
	public ResponseEntity<BaseResponse<EmailSendResponseDto>> sendVerificationCode(
		@Valid @RequestBody EmailSendRequestDto request
	) {
		EmailSendResponseDto response = emailVerificationService.sendVerificationCode(request);
		return BaseResponse.ok("인증 코드가 이메일로 발송되었습니다.", response, HttpStatus.OK);
	}


	@PostMapping("/verify/email-check")
	public ResponseEntity<BaseResponse<Void>> confirmVerificationCode(
		@Valid @RequestBody EmailVerificationDto request
	) {
		emailVerificationService.verifyCode(request.email(), request.verificationCode());
		return BaseResponse.ok("이메일 인증이 완료되었습니다.", null, HttpStatus.OK);
	}

	//SecurityContext에 잘 올라간 지 테스트
	@GetMapping("/test-context")
	public String testSecurityContext(Principal principal, HttpSession session) {
		Long sessionUserId = (Long) session.getAttribute("LOGIN_USER");

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Long contextUserId = null;
		if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
			contextUserId = (Long) auth.getPrincipal();
		}

		Long principalUserId = null;
		if (principal != null) {
			try {
				principalUserId = Long.valueOf(principal.getName());
			} catch (NumberFormatException e) {
				principalUserId = null;
			}
		}

		return String.format("Session: %s, Context: %s, Principal: %s",
			sessionUserId, contextUserId, principalUserId);
	}
}
