package com.threestar.trainus.domain.coupon.admin.mapper;

import com.threestar.trainus.domain.coupon.admin.dto.CouponCreateRequestDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponCreateResponseDto;
import com.threestar.trainus.domain.coupon.user.entity.Coupon;

public class AdminCouponMapper {

	private AdminCouponMapper() {
	}

	public static Coupon toEntity(CouponCreateRequestDto request) {
		return Coupon.builder()
			.name(request.couponName())
			.expirationDate(request.expirationDate())
			.discountPrice(request.discountPrice())
			.minOrderPrice(request.minOrderPrice())
			.status(request.status())
			.quantity(request.quantity())
			.category(request.category())
			.openAt(request.openTime())
			.closeAt(request.closeTime())
			.build();
	}

	public static CouponCreateResponseDto toCreateResponseDto(Coupon coupon) {
		return new CouponCreateResponseDto(
			coupon.getId(),
			coupon.getName(),
			coupon.getStatus().name(),
			coupon.getCreatedAt()
		);
	}
}
