package com.threestar.trainus.domain.payment.dto.failure;

import java.util.List;

import lombok.Builder;

@Builder
public record PaymentFailureHistoryPageDto(
	Integer count,
	List<PaymentFailureHistoryResponseDto> failureHistory
) {
}
