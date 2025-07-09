package com.threestar.trainus.domain.coupon.mapper;

import com.threestar.trainus.domain.coupon.dto.CreateUserCouponResponseDto;
import com.threestar.trainus.domain.coupon.entity.UserCoupon;

public class UserCouponMapper {

	public static CreateUserCouponResponseDto toCreateUserCouponResponseDto(UserCoupon userCoupon) {
		return CreateUserCouponResponseDto.builder()
			.couponId(userCoupon.getCoupon().getId())
			.userId(userCoupon.getUser().getId())
			.createdAt(userCoupon.getCreatedAt())
			.expirationDate(userCoupon.getExpirationDate())
			.status(userCoupon.getStatus().name())
			.build();
	}
}
