package com.threestar.trainus.domain.coupon.admin.dto;

import java.time.LocalDateTime;

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
	String status,
	Integer quantity,
	String category,
	LocalDateTime couponOpenAt,
	LocalDateTime couponDeadlineAt
) {
}
