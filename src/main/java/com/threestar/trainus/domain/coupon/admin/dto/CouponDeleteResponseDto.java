package com.threestar.trainus.domain.coupon.admin.dto;

import java.time.LocalDateTime;

/**
 * 쿠폰 삭제 응답 DTO
 */
public record CouponDeleteResponseDto(
	Long couponId,
	String couponName,
	LocalDateTime deletedAt
) {
}