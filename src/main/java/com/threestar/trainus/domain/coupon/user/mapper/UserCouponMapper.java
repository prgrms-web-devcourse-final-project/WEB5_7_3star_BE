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
		return CreateUserCouponResponseDto.builder()
			.couponId(userCoupon.getCoupon().getId())
			.userId(userCoupon.getUser().getId())
			.createdAt(userCoupon.getCreatedAt())
			.expirationDate(userCoupon.getExpirationDate())
			.status(userCoupon.getStatus())
			.build();
	}

	public static UserCouponResponseDto toUserCouponResponseDto(UserCoupon userCoupon) {
		Coupon coupon = userCoupon.getCoupon();
		return UserCouponResponseDto.builder()
			.couponId(coupon.getId())
			.couponName(coupon.getName())
			.discountPrice(coupon.getDiscountPrice())
			.minOrderPrice(coupon.getMinOrderPrice())
			.expirationDate(coupon.getExpirationDate())
			.status(coupon.getStatus())
			.useDate(userCoupon.getUseDate())
			.build();
	}

	public static List<UserCouponResponseDto> toDtoList(List<UserCoupon> userCoupons) {
		return userCoupons.stream()
			.map(UserCouponMapper::toUserCouponResponseDto)
			.toList();
	}
}
