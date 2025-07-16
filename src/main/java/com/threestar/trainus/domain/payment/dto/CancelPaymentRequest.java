package com.threestar.trainus.domain.payment.dto;

import lombok.Data;

@Data
public class CancelPaymentRequest {
	private String paymentKey;
	private String cancelReason;
}
