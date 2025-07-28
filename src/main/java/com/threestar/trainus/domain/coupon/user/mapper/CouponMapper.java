package com.threestar.trainus.domain.coupon.user.mapper;

import java.util.List;

import com.threestar.trainus.domain.coupon.user.dto.CouponResponseDto;
import com.threestar.trainus.domain.coupon.user.entity.Coupon;
import com.threestar.trainus.domain.coupon.user.entity.OwnedStatus;

public class CouponMapper {

	public static CouponResponseDto toDto(Coupon coupon, boolean owned) {
		return new CouponResponseDto(
			coupon.getId(),
			coupon.getName(),
			coupon.getDiscountPrice(),
			coupon.getMinOrderPrice(),
			coupon.getExpirationDate(),
			owned ? OwnedStatus.OWNED : OwnedStatus.NOT_OWNED,
			coupon.getQuantity(),
			coupon.getCategory(),
			coupon.getOpenAt()
		);
	}

	public static List<CouponResponseDto> toDtoList(List<Coupon> coupons, List<Long> ownedCouponIds) {
		return coupons.stream()
			.map(c -> toDto(c, ownedCouponIds.contains(c.getId())))
			.toList();
	}
}
