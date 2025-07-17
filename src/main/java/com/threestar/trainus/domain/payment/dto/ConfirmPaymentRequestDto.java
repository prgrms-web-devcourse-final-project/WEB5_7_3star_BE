package com.threestar.trainus.domain.payment.dto;

import jakarta.validation.constraints.NotNull;

public record ConfirmPaymentRequestDto(
	int amount,
	@NotNull(message = "필수 값입니다.")
	String orderId,
	@NotNull(message = "필수 값입니다.")
	String paymentKey
) {

}
