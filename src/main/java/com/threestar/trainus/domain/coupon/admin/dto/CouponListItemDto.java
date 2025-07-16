package com.threestar.trainus.domain.coupon.admin.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;

/**
 * 개별의 쿠폰 정보
 */
public record CouponListItemDto(
	Long couponId,
	String couponName,
	LocalDateTime expirationDate,
	String discountPrice,
	Integer minOrderPrice,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	CouponStatus status,
	Integer quantity,
	CouponCategory category,
	LocalDateTime couponOpenAt,
	LocalDateTime couponDeadlineAt
) {
}
