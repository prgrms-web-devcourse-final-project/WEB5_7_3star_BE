package com.threestar.trainus.domain.profile.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.profile.dto.IntroUpdateRequestDto;
import com.threestar.trainus.domain.profile.dto.ProfileDetailResponseDto;
import com.threestar.trainus.domain.profile.dto.ImageUpdateResponseDto;
import com.threestar.trainus.domain.profile.dto.ImageUpdateRequestDto;
import com.threestar.trainus.domain.profile.dto.IntroUpdateResponseDto;
import com.threestar.trainus.domain.profile.service.ProfileFacadeService;
import com.threestar.trainus.global.annotation.LoginUser;
import com.threestar.trainus.global.unit.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "유저 프로필 API", description = "유저 프로필 조회/수정 API")
@RestController
@RequestMapping("api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

	private final ProfileFacadeService facadeService;

	@GetMapping("{userId}")
	@Operation(summary = "유저 프로필 상세 조회 api")
	public ResponseEntity<BaseResponse<ProfileDetailResponseDto>> getProfileDetail(
		@PathVariable Long userId
	) {
		ProfileDetailResponseDto response = facadeService.getProfileDetail(userId);
		return BaseResponse.ok("프로필 상세 조회가 완료되었습니다.", response, HttpStatus.OK);
	}

	@PatchMapping("/image")
	@Operation(summary = "유저 프로필 이미지 수정 api")
	public ResponseEntity<BaseResponse<ImageUpdateResponseDto>> updateProfileImage(
		@Valid @RequestBody ImageUpdateRequestDto requestDto,
		@LoginUser Long loginUserId
	) {
		ImageUpdateResponseDto response = facadeService.updateProfileImage(loginUserId, requestDto);
		return BaseResponse.ok("프로필 이미지 수정이 완료되었습니다.", response, HttpStatus.OK);
	}

	@PatchMapping("/intro")
	@Operation(summary = "유저 프로필 자기소개 수정 api")
	public ResponseEntity<BaseResponse<IntroUpdateResponseDto>> updateProfileIntro(
		@Valid @RequestBody IntroUpdateRequestDto requestDto,
		@LoginUser Long loginUserId
	) {
		IntroUpdateResponseDto response = facadeService.updateProfileIntro(loginUserId, requestDto);
		return BaseResponse.ok("프로필 자기소개 수정이 완료되었습니다.", response, HttpStatus.OK);
	}
}
