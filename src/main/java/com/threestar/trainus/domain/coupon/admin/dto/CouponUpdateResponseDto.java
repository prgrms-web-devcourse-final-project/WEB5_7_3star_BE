package com.threestar.trainus.domain.coupon.admin.dto;

import java.time.LocalDateTime;

/**
 * 쿠폰 수정 응답 DTO
 */
public record CouponUpdateResponseDto(
	String couponName,
	String status,
	Integer quantity,
	String category,
	LocalDateTime couponOpenAt,
	LocalDateTime couponDeadlineAt,
	LocalDateTime updatedAt
) {
}
