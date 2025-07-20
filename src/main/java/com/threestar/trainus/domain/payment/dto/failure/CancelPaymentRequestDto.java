package com.threestar.trainus.domain.payment.dto;

import jakarta.validation.constraints.NotNull;

public record CancelPaymentRequestDto(
	@NotNull(message = "필수 값입니다.")
	String paymentKey,
	@NotNull(message = "실패 원인은 필수입니다.")
	String cancelReason
) {
}
