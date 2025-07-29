package com.threestar.trainus.domain.coupon.user.mapper;

import java.util.List;

import com.threestar.trainus.domain.coupon.user.dto.CreateUserCouponResponseDto;
import com.threestar.trainus.domain.coupon.user.dto.UserCouponResponseDto;
import com.threestar.trainus.domain.coupon.user.entity.Coupon;
import com.threestar.trainus.domain.coupon.user.entity.UserCoupon;

public final class UserCouponMapper {

	private UserCouponMapper() {
	}

	public static CreateUserCouponResponseDto toCreateUserCouponResponseDto(UserCoupon userCoupon) {
		return new CreateUserCouponResponseDto(
			userCoupon.getCoupon().getId(),
			userCoupon.getUser().getId(),
			userCoupon.getCreatedAt(),
			userCoupon.getExpirationDate(),
			userCoupon.getStatus()
		);
	}

	public static UserCouponResponseDto toUserCouponResponseDto(UserCoupon userCoupon) {
		Coupon coupon = userCoupon.getCoupon();
		return new UserCouponResponseDto(
			coupon.getId(),
			coupon.getName(),
			coupon.getDiscountPrice(),
			coupon.getMinOrderPrice(),
			coupon.getExpirationDate(),
			coupon.getStatus(),
			userCoupon.getUseDate()
		);
	}

	public static List<UserCouponResponseDto> toDtoList(List<UserCoupon> userCoupons) {
		return userCoupons.stream()
			.map(UserCouponMapper::toUserCouponResponseDto)
			.toList();
	}
}
