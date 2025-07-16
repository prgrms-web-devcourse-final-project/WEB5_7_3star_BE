package com.threestar.trainus.domain.coupon.admin.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;

/**
 * 쿠폰 수정 응답 DTO
 */
public record CouponUpdateResponseDto(
	String couponName,
	CouponStatus status,
	Integer quantity,
	CouponCategory category,
	LocalDateTime couponOpenAt,
	LocalDateTime couponDeadlineAt,
	LocalDateTime updatedAt
) {
}
