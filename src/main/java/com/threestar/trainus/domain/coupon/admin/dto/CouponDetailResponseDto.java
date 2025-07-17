package com.threestar.trainus.domain.coupon.admin.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;

/**
 * 쿠폰 상세 조회 응답 DTO
 */
public record CouponDetailResponseDto(
	Long id,
	String couponName,
	LocalDateTime expirationDate,
	String discountPrice,
	Integer minOrderPrice,
	CouponStatus status,
	Integer quantity,
	CouponCategory couponCategory,
	LocalDateTime couponOpenAt,
	LocalDateTime couponDeadlineAt,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	Integer issuedCount
) {
}
