package com.threestar.trainus.domain.coupon.user.dto;

import java.util.List;

public record CouponPageResponseDto(
	List<CouponResponseDto> coupons
) {
}
