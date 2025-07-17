package com.threestar.trainus.domain.payment.dto;

import lombok.Data;

@Data
public class SaveAmountRequest {
	private String orderId;
	private Integer amount;
}
