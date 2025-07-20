package com.threestar.trainus.domain.payment.dto.failure;

public record CancelDetail(
	String cancelReason,
	String canceledAt,
	int cancelAmount,
	int refundableAmount
) {
}
