package com.threestar.trainus.domain.profile.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.profile.dto.ProfileDetailResponseDto;
import com.threestar.trainus.domain.profile.dto.ProfileResponseDto;
import com.threestar.trainus.domain.profile.dto.ProfileUpdateRequestDto;
import com.threestar.trainus.domain.profile.service.ProfileFacadeService;
import com.threestar.trainus.global.unit.BaseResponse;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

	private final ProfileFacadeService facadeService;

	@GetMapping("{userId}")
	public ResponseEntity<BaseResponse<ProfileDetailResponseDto>> getProfileDetail(
		@PathVariable Long userId
	) {
		ProfileDetailResponseDto response = facadeService.getProfileDetail(userId);
		return BaseResponse.ok("프로필 상세 조회가 완료되었습니다.", response, HttpStatus.OK);
	}

	@PatchMapping
	public ResponseEntity<BaseResponse<ProfileResponseDto>> updateProfile(
		@Valid @RequestBody ProfileUpdateRequestDto requestDto,
		HttpSession session
	) {
		Long userId = (Long)session.getAttribute("LOGIN_USER");
		ProfileResponseDto response = facadeService.updateProfile(userId, requestDto);
		return BaseResponse.ok("프로필 수정이 완료되었습니다.", response, HttpStatus.OK);
	}
}
