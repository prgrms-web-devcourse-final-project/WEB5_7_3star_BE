package com.threestar.trainus.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailVerificationConfirmDto(

	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	String email,

	@NotBlank(message = "인증 코드는 필수입니다.")
	@Size(min = 6, max = 6, message = "인증 코드는 6자리여야 합니다.")
	String code
) {
}
