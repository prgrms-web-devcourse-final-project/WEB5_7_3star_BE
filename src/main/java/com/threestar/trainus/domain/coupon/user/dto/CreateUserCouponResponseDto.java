package com.threestar.trainus.domain.coupon.user.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateUserCouponResponseDto {
	private Long couponId;
	private Long userId;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime createdAt;
	private LocalDateTime expirationDate;
	private CouponStatus status;

}
