package com.threestar.trainus.domain.payment.dto.success;

import java.util.List;

import lombok.Builder;

@Builder
public record PaymentSuccessHistoryPageDto(
	Integer count,
	List<PaymentSuccessHistoryResponseDto> successHistory
) {
}
