package com.threestar.trainus.domain.coupon.admin.dto;

import java.time.LocalDateTime;

public record CouponCreateResponseDto(
	Long couponId,
	String couponName,
	String status,
	LocalDateTime createdAt
) {
}
