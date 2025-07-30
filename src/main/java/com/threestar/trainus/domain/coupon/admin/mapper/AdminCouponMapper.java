package com.threestar.trainus.domain.coupon.admin.mapper;

import java.util.List;

import com.threestar.trainus.domain.coupon.admin.dto.CouponCreateRequestDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponCreateResponseDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponDeleteResponseDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponDetailResponseDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponListItemDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponListResponseDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponListWrapperDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponUpdateResponseDto;
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
			.openAt(request.couponOpenAt())
			.closeAt(request.couponDeadlineAt())
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

	public static CouponListItemDto toCouponListItemDto(Coupon coupon) {
		return new CouponListItemDto(
			coupon.getId(),
			coupon.getName(),
			coupon.getExpirationDate(),
			coupon.getDiscountPrice(),
			coupon.getMinOrderPrice(),
			coupon.getCreatedAt(),
			coupon.getUpdatedAt(),
			coupon.getStatus(),
			coupon.getQuantity(),
			coupon.getCategory(),
			coupon.getOpenAt(),
			coupon.getCloseAt()
		);
	}

	public static CouponListResponseDto toCouponListResponseDto(
		List<Coupon> coupons, int totalCount
	) {
		return new CouponListResponseDto(
			totalCount,
			coupons.stream()
				.map(AdminCouponMapper::toCouponListItemDto)
				.toList()
		);
	}

	public static CouponDetailResponseDto toCouponDetailResponseDto(Coupon coupon, Integer issuedCount) {
		return new CouponDetailResponseDto(
			coupon.getId(),
			coupon.getName(),
			coupon.getExpirationDate(),
			coupon.getDiscountPrice(),
			coupon.getMinOrderPrice(),
			coupon.getStatus(),
			coupon.getQuantity(),
			coupon.getCategory(),
			coupon.getOpenAt(),
			coupon.getCloseAt(),
			coupon.getCreatedAt(),
			coupon.getUpdatedAt(),
			issuedCount
		);
	}

	public static CouponUpdateResponseDto toCouponUpdateResponseDto(Coupon coupon) {
		return new CouponUpdateResponseDto(
			coupon.getName(),
			coupon.getStatus(),
			coupon.getQuantity(),
			coupon.getCategory(),
			coupon.getOpenAt(),
			coupon.getCloseAt(),
			coupon.getUpdatedAt()
		);
	}

	public static CouponDeleteResponseDto toCouponDeleteResponseDto(Coupon coupon) {
		return new CouponDeleteResponseDto(
			coupon.getId(),
			coupon.getName(),
			coupon.getDeletedAt()
		);
	}

	public static CouponListWrapperDto toCouponListWrapperDto(CouponListResponseDto couponsInfo) {
		return new CouponListWrapperDto(couponsInfo.couponList());
	}
}
