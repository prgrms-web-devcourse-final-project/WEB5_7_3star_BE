package com.threestar.trainus.domain.payment.dto.cancel;

import java.util.List;

import lombok.Builder;

@Builder
public record PaymentCancelPageWrapperDto(
	List<PaymentCancelHistoryResponseDto> failureHistory
) {
}
