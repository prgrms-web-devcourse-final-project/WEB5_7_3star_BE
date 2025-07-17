package com.threestar.trainus.domain.payment.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.payment.entity.PaymentStatus;

import lombok.Getter;

@Getter
public class TossPaymentResponseDto {
	private String paymentKey;
	private String orderId;
	private String orderName;
	private String status;
	private String requestedAt;
	private String approvedAt;
	private int totalAmount;
	private String method;
}
