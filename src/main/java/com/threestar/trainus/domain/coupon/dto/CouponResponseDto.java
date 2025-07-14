package com.threestar.trainus.domain.coupon.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.coupon.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.entity.OwnedStatus;

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
	private String category;
	private LocalDateTime openTime;

	public CouponResponseDto(Long couponId, String couponName, String discountPrice,
		Integer minOrderPrice, LocalDateTime expirationDate,
		String ownedStatus, Integer quantity, CouponCategory category,
		LocalDateTime openTime) {
		this.couponId = couponId;
		this.couponName = couponName;
		this.discountPrice = discountPrice;
		this.minOrderPrice = minOrderPrice;
		this.expirationDate = expirationDate;
		this.ownedStatus = OwnedStatus.valueOf(ownedStatus);
		this.quantity = quantity;
		this.category = category.name();
		this.openTime = openTime;
	}
}
