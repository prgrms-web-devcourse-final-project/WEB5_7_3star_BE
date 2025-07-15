package com.threestar.trainus.domain.coupon.admin.dto;

import java.util.List;

public record CouponListResponseDto(
	Long totalCount,
	List<CouponListItemDto> couponList
) {
}
