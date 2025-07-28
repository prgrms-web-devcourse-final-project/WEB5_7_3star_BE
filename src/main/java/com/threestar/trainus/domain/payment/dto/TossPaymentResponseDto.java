package com.threestar.trainus.domain.payment.dto;

import java.util.List;

import com.threestar.trainus.domain.payment.dto.cancel.CancelDetail;

public record TossPaymentResponseDto(
	String paymentKey,
	String orderId,
	String orderName,
	String status,
	String requestedAt,
	String approvedAt,
	int totalAmount,
	String method,
	List<CancelDetail> cancels
) {
}
