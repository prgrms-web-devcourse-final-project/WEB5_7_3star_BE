package com.threestar.trainus.domain.payment.dto;

import jakarta.validation.constraints.NotNull;

public record PaymentRequestDto(
	@NotNull(message = "필수 값입니다.")
	Long lessonId,
	Long userCouponId
) {
}
