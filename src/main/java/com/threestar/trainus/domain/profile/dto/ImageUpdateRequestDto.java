package com.threestar.trainus.domain.profile.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ImageUpdateRequestDto(

	@Size(max = 2048, message = "프로필 이미지 URL은 2048자 이하여야 합니다.")
	@Pattern(
		regexp = "^$|^https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+$",
		message = "올바른 URL 형식이어야 합니다."
	)
	String profileImage
) {
}
