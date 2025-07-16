package com.threestar.trainus.domain.coupon.admin.dto;

import java.time.LocalDateTime;

/**
 * 쿠폰 상세 조회 응답 DTO
 */
public record CouponDetailResponseDto(
	Long id,
	String couponName,
	LocalDateTime expirationDate,
	String discountPrice,
	Integer minOrderPrice,
	String status,
	Integer quantity,
	String couponCategory,
	LocalDateTime couponOpenAt,
	LocalDateTime couponDeadlineAt,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	Integer issuedCount
) {
}
