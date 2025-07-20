package com.threestar.trainus.domain.payment.dto.failure;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.payment.entity.PaymentMethod;
import com.threestar.trainus.domain.payment.entity.PaymentStatus;

import lombok.Builder;

@Builder
public record PaymentFailureHistoryResponseDto(
	String lessonTitle,
	LocalDateTime paymentCancelledAt,
	LocalDateTime lessonStartAt,
	LocalDateTime lessonEndAt,
	PaymentMethod paymentMethod,
	String city,
	String district,
	String dong,
	Integer payPrice,
	Integer refundPrice,
	PaymentStatus paymentStatus,
	String paymentKey,
	String orderId,
	String detailAddress,
	String cancelReason
) {
}
