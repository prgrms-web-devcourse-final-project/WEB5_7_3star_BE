package com.threestar.trainus.domain.coupon.admin.dto;

import java.util.List;

public record CouponListWrapperDto(
	List<CouponListItemDto> coupons
) {
}
