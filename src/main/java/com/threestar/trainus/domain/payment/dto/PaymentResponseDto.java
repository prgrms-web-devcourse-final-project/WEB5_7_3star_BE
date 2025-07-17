package com.threestar.trainus.domain.payment.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.payment.entity.PaymentMethod;

import lombok.Builder;

@Builder
public record PaymentResponseDto(
	String orderId,
	String lessonTitle,
	Integer originPrice,
	Integer payPrice,
	PaymentMethod paymentMethod,
	LocalDateTime expiredAt
) {
}
