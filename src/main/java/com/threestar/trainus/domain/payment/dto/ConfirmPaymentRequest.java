package com.threestar.trainus.domain.payment.dto;

import lombok.Data;

@Data
public class ConfirmPaymentRequest {
	private Integer amount;
	private String orderId;
	private String paymentKey;
}
