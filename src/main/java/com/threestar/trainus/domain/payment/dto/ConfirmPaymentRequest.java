package com.threestar.trainus.domain.payment.dto;

import lombok.Data;

@Data
public class ConfirmPaymentRequest {
	private int amount;
	private String orderId;
	private String paymentKey;
}
