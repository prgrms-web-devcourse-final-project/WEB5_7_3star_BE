package com.threestar.trainus.domain.coupon.admin.mapper;

import org.springframework.data.domain.Page;

import com.threestar.trainus.domain.coupon.admin.dto.CouponCreateRequestDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponCreateResponseDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponDetailResponseDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponListItemDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponListResponseDto;
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
			coupon.getStatus().name(),
			coupon.getQuantity(),
			coupon.getCategory().name(),
			coupon.getOpenAt(),
			coupon.getCloseAt()
		);
	}

	public static CouponListResponseDto toCouponListResponseDto(Page<Coupon> couponPage) {
		return new CouponListResponseDto(
			couponPage.getTotalElements(),
			couponPage.getContent().stream()
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
			coupon.getStatus().name(),
			coupon.getQuantity(),
			coupon.getCategory().name(),
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
			coupon.getStatus().name(),
			coupon.getQuantity(),
			coupon.getCategory().name(),
			coupon.getOpenAt(),
			coupon.getCloseAt(),
			coupon.getUpdatedAt()
		);
	}
}
