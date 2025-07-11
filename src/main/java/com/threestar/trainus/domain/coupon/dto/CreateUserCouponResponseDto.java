package com.threestar.trainus.domain.coupon.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

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
	private String status;

}
