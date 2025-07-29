package com.threestar.trainus.domain.coupon.user.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;

public record CreateUserCouponResponseDto(
	Long couponId,
	Long userId,
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime createdAt,
	LocalDateTime expirationDate,
	CouponStatus status
) {
}
