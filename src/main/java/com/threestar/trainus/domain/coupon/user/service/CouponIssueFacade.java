package com.threestar.trainus.domain.coupon.user.service;

import com.threestar.trainus.domain.coupon.user.dto.CreateUserCouponResponseDto;
import com.threestar.trainus.global.annotation.DistributedLock;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponIssueFacade {

	private final CouponService couponService;

	@DistributedLock(key = "'coupon:' + #couponId")
	public CreateUserCouponResponseDto issueCoupon(Long userId, Long couponId) {
		return couponService.issueCoupon(userId, couponId);
	}
}
