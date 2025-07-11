package com.threestar.trainus.domain.coupon.dto;

import java.time.LocalDateTime;

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
	private String status;
	private LocalDateTime useDate;
}
