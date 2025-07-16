package com.threestar.trainus.domain.coupon.user.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.user.entity.OwnedStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CouponResponseDto {
	private Long couponId;
	private String couponName;
	private String discountPrice;
	private Integer minOrderPrice;
	private LocalDateTime expirationDate;
	private OwnedStatus ownedStatus;
	private Integer quantity;
	private CouponCategory category;
	private LocalDateTime openTime;

}
