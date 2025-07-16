package com.threestar.trainus.domain.coupon.admin.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CouponCreateRequestDto(

	@NotBlank(message = "쿠폰명은 필수입니다")
	@Size(max = 45, message = "쿠폰명은 45자 이하여야 합니다")
	String couponName,

	LocalDateTime expirationDate,

	@NotBlank(message = "할인가격은 필수입니다")
	String discountPrice,

	@NotNull(message = "최소 주문 금액은 필수입니다")
	@Min(value = 0, message = "최소 주문 금액은 0원 이상이어야 합니다")
	Integer minOrderPrice,

	@NotNull(message = "쿠폰 상태는 필수입니다")
	CouponStatus status,

	@Min(value = 1, message = "수량은 1개 이상이어야 합니다")
	Integer quantity,

	@NotNull(message = "쿠폰 카테고리는 필수입니다")
	CouponCategory category,

	@NotNull(message = "오픈 시간은 필수입니다")
	LocalDateTime couponOpenAt,

	@NotNull(message = "마감 시간은 필수입니다")
	LocalDateTime couponDeadlineAt
) {
}
