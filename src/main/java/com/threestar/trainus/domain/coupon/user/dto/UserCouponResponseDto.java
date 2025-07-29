package com.threestar.trainus.domain.coupon.user.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;

public record UserCouponResponseDto(
	Long couponId,
	String couponName,
	String discountPrice,
	Integer minOrderPrice,
	LocalDateTime expirationDate,
	CouponStatus status,
	LocalDateTime useDate
) {
}
