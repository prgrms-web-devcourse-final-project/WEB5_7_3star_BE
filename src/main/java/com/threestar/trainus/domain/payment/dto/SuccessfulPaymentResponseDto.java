package com.threestar.trainus.domain.payment.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.payment.entity.PaymentMethod;

import lombok.Builder;

@Builder
public record SuccessfulPaymentResponseDto(
	String addressDetail,
	LocalDateTime startAt,
	LocalDateTime endAt,
	int payPrice,
	String city,
	String district,
	String dong,
	PaymentMethod paymentMethod
) {
}
