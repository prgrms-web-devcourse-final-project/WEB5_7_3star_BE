package com.threestar.trainus.domain.payment.dto.cancel;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record CancelPaymentResponseDto(
	String lessonName,
	String cancelReason,
	LocalDateTime startAt,
	LocalDateTime endAt,
	LocalDateTime paymentCancelledAt,
	int payPrice,
	int refundableAmount
) {
}
