package com.threestar.trainus.domain.payment.dto;

import com.threestar.trainus.domain.payment.entity.PaymentMethod;

import lombok.Data;

@Data
public class PaymentRequestDto {
	private Long lessonId;
	private Long userCouponId;
	private PaymentMethod paymentMethod;
}
