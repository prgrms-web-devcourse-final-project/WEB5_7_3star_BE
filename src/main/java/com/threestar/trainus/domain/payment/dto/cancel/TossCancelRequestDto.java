package com.threestar.trainus.domain.payment.dto.failure;

public record TossCancelRequestDto(
	String paymentKey,
	String cancelReason,
	int cancelAmount
) {
}
