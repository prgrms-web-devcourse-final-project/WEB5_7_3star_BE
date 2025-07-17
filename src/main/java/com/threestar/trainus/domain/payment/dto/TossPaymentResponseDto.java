package com.threestar.trainus.domain.payment.dto;

public record TossPaymentResponseDto(
	String paymentKey,
	String orderId,
	String orderName,
	String status,
	String requestedAt,
	String approvedAt,
	int totalAmount,
	String method
) {
}
