package com.threestar.trainus.domain.coupon.dto;

import java.time.LocalDateTime;

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

}
