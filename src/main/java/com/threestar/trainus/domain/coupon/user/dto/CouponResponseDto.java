package com.threestar.trainus.domain.coupon.user.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.user.entity.OwnedStatus;

public record CouponResponseDto(
	Long couponId,
	String couponName,
	String discountPrice,
	Integer minOrderPrice,
	LocalDateTime expirationDate,
	OwnedStatus ownedStatus,
	Integer quantity,
	CouponCategory category,
	LocalDateTime openTime
) {
}
