package com.threestar.trainus.domain.coupon.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.coupon.entity.OwnedStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CouponResponseDto {
	private Long couponId;
	private String couponName;
	private String discountPrice;
	private Integer minOrderPrice;
	private LocalDateTime expirationDate;
	private OwnedStatus ownedStatus;
	private Integer quantity;
	private String category;
	private LocalDateTime openTime;
}
