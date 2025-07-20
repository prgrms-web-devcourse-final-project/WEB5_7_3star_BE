package com.threestar.trainus.domain.payment.dto.failure;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record FailurePaymentResponseDto(
	String lessonName,
	String cancelReason,
	LocalDateTime startAt,
	LocalDateTime endAt,
	LocalDateTime paymentCancelledAt,
	int payPrice,
	int refundableAmount
) {
}
