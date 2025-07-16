package com.threestar.trainus.domain.coupon.admin.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 쿠폰 수정 요청 DTO
 * 일반쿠폰(NORMAL): 수량은 프론트에서 비활성화, 백엔드에서 자동으로 null 처리
 * 선착순쿠폰(OPEN_RUN): 수량 필수 입력
 */
public record CouponUpdateRequestDto(

	@Size(max = 45, message = "쿠폰명은 45자 이하여야 합니다")
	String couponName,

	CouponStatus status,

	@Min(value = 1, message = "수량은 1개 이상이어야 합니다")
	Integer quantity,

	CouponCategory category,

	LocalDateTime couponOpenAt,

	LocalDateTime couponDeadlineAt
) {
}
