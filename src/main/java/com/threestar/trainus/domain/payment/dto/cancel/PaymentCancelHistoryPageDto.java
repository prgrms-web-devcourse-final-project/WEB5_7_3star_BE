package com.threestar.trainus.domain.payment.dto.cancel;

import java.util.List;

import lombok.Builder;

@Builder
public record PaymentCancelHistoryPageDto(
	Integer count,
	List<PaymentCancelHistoryResponseDto> failureHistory
) {
}
