package com.threestar.trainus.domain.coupon.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CouponPageResponseDto {
	private List<CouponResponseDto> coupons;
}
