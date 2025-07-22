package com.threestar.trainus.domain.payment.dto.cancel;

public record TossCancelRequestDto(
	String paymentKey,
	String cancelReason,
	int cancelAmount
) {
}
