package com.threestar.trainus.domain.coupon.mapper;

import java.util.List;

import com.threestar.trainus.domain.coupon.dto.CouponResponseDto;
import com.threestar.trainus.domain.coupon.entity.Coupon;
import com.threestar.trainus.domain.coupon.entity.OwnedStatus;

public class CouponMapper {

	public static CouponResponseDto toDto(Coupon coupon, boolean owned) {
		return CouponResponseDto.builder()
			.couponId(coupon.getId())
			.couponName(coupon.getName())
			.discountPrice(coupon.getDiscountPrice())
			.minOrderPrice(coupon.getMinOrderPrice())
			.expirationDate(coupon.getExpirationDate())
			.ownedStatus(owned ? OwnedStatus.OWNED : OwnedStatus.NOT_OWNED)
			.quantity(coupon.getQuantity())
			.category(coupon.getCategory().name())
			.openTime(coupon.getOpenAt())
			.build();
	}

	public static List<CouponResponseDto> toDtoList(List<Coupon> coupons, List<Long> ownedCouponIds) {
		return coupons.stream()
			.map(c -> toDto(c, ownedCouponIds.contains(c.getId())))
			.toList();
	}
}
