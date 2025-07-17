package com.threestar.trainus.domain.coupon.user.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserCouponResponseDto {
	private Long couponId;
	private String couponName;
	private String discountPrice;
	private Integer minOrderPrice;
	private LocalDateTime expirationDate;
	private CouponStatus status;
	private LocalDateTime useDate;
}
