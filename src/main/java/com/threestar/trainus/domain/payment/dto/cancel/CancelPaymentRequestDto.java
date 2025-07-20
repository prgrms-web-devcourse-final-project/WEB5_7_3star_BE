package com.threestar.trainus.domain.payment.dto.failure;

import jakarta.validation.constraints.NotNull;

public record CancelPaymentRequestDto(
	@NotNull(message = "필수 값입니다.")
	String orderId,
	@NotNull(message = "실패 원인은 필수입니다.")
	String cancelReason
) {
}
