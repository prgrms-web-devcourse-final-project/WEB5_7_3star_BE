package com.threestar.trainus.domain.payment.dto.cancel;

public record CancelDetail(
	String cancelReason,
	String canceledAt,
	int cancelAmount,
	int refundableAmount
) {
}
