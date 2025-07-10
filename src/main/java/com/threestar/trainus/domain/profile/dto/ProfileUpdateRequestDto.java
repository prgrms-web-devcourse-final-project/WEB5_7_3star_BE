package com.threestar.trainus.domain.profile.dto;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequestDto(

	@Size(max = 255, message = "프로필 이미지 URL은 255자 이하여야 합니다.")
	String profileImage,

	@Size(max = 255, message = "자기소개는 255자 이하여야 합니다.")
	String intro
) {
}
