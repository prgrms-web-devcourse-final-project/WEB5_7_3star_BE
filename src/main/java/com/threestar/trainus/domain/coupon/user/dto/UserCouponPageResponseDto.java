package com.threestar.trainus.domain.coupon.user.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserCouponPageResponseDto {
	private List<UserCouponResponseDto> userCoupons;
}
