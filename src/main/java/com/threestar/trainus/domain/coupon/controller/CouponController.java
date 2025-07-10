package com.threestar.trainus.domain.coupon.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.coupon.dto.CreateUserCouponResponseDto;
import com.threestar.trainus.domain.coupon.dto.UserCouponPageResponseDto;
import com.threestar.trainus.domain.coupon.entity.CouponStatus;
import com.threestar.trainus.domain.coupon.service.CouponService;
import com.threestar.trainus.global.unit.BaseResponse;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/coupons")
public class CouponController {

	private final CouponService couponService;

	@PostMapping("/{couponId}")
	public ResponseEntity<BaseResponse<CreateUserCouponResponseDto>> createUserCoupon(@PathVariable Long couponId,
		HttpSession session
	) {
		Long userId = (Long)session.getAttribute("LOGIN_USER");
		CreateUserCouponResponseDto dto = couponService.createUserCoupon(userId, couponId);

		return BaseResponse.ok("쿠폰 발급 완료", dto, HttpStatus.CREATED);
	}

	@GetMapping("/my-coupons")
	public ResponseEntity<BaseResponse<UserCouponPageResponseDto>> getUserCoupons(
		@RequestParam(required = false) CouponStatus status, HttpSession session
	) {
		Long userId = (Long)session.getAttribute("LOGIN_USER");
		UserCouponPageResponseDto dto = couponService.getUserCoupons(userId, status);

		return BaseResponse.ok("사용자 보유 쿠폰 조회 성공", dto, HttpStatus.OK);
	}
}
