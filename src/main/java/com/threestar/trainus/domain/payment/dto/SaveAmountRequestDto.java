package com.threestar.trainus.domain.payment.dto;

public record SaveAmountRequestDto(
	String orderId,
	int amount
) {
}
