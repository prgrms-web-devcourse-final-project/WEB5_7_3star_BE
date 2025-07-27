package com.threestar.trainus.domain.payment.dto.success;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.payment.entity.PaymentMethod;
import com.threestar.trainus.domain.payment.entity.PaymentStatus;

import lombok.Builder;

@Builder
public record PaymentSuccessHistoryResponseDto(
	String lessonTitle,
	LocalDateTime paymentApprovedAt,
	LocalDateTime lessonStartAt,
	LocalDateTime lessonEndAt,
	PaymentMethod paymentMethod,
	String city,
	String district,
	String dong,
	Integer payPrice,
	PaymentStatus paymentStatus,
	String orderId,
	String detailAddress
) {
}
