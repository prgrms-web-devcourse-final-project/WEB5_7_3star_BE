package com.threestar.trainus.domain.coupon.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.coupon.entity.UserCoupon;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateUserCouponResponseDto {
	private Long couponId;
	private Long userId;
	private LocalDateTime createdAt;
	private LocalDateTime expirationDate;
	private String status;

	public static CreateUserCouponResponseDto from(UserCoupon userCoupon) {
		return CreateUserCouponResponseDto.builder()
			.couponId(userCoupon.getCoupon().getId())
			.userId(userCoupon.getUser().getId())
			.createdAt(userCoupon.getCreatedAt())
			.expirationDate(userCoupon.getExpirationDate())
			.status(userCoupon.getStatus().name())
			.build();
	}
}
