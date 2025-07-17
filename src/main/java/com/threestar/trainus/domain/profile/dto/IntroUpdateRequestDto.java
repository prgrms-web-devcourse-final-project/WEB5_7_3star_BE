package com.threestar.trainus.domain.profile.dto;

import jakarta.validation.constraints.Size;

public record IntroUpdateRequestDto(

	@Size(max = 255, message = "자기소개는 255자 이하여야 합니다.")
	String intro
) {
}
