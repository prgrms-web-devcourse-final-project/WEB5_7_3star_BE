package com.threestar.trainus.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NicknameCheckRequestDto(

	@NotBlank(message = "닉네임은 필수입니다")
	@Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다")
	@Pattern(regexp = "^[a-zA-Z0-9가-힣]*$", message = "닉네임은 한글, 영문, 숫자만 사용 가능합니다")
	String nickname
) {
}
