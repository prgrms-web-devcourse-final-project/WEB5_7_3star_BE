package com.threestar.trainus.domain.coupon.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.coupon.user.dto.CouponPageResponseDto;
import com.threestar.trainus.domain.coupon.user.dto.CreateUserCouponResponseDto;
import com.threestar.trainus.domain.coupon.user.dto.UserCouponPageResponseDto;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;
import com.threestar.trainus.domain.coupon.user.service.CouponService;
import com.threestar.trainus.global.annotation.LoginUser;
import com.threestar.trainus.global.unit.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "쿠폰 API", description = "쿠폰 발급/조회 관련 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/coupons")
public class CouponController {

	private final CouponService couponService;

	@PostMapping("/{couponId}")
	@Operation(summary = "쿠폰 발급 API", description = "couponId에 맞는 쿠폰을 유저에게 발급하는 API입니다.")
	public ResponseEntity<BaseResponse<CreateUserCouponResponseDto>> createUserCoupon(
		@PathVariable Long couponId,
		@LoginUser Long userId
	) {
		CreateUserCouponResponseDto dto = couponService.createUserCouponWithDistributedLock(userId, couponId);

		return BaseResponse.ok("쿠폰 발급 완료", dto, HttpStatus.CREATED);
	}

	@GetMapping("/my-coupons")
	@Operation(summary = "내 쿠폰 목록 조회 API", description = "내가 보유한 쿠폰 목록을 조회하는 API입니다.")
	public ResponseEntity<BaseResponse<UserCouponPageResponseDto>> getUserCoupons(
		@RequestParam(required = false) CouponStatus status,
		@LoginUser Long userId
	) {
		UserCouponPageResponseDto dto = couponService.getUserCoupons(userId, status);

		return BaseResponse.ok("사용자 보유 쿠폰 조회 성공", dto, HttpStatus.OK);
	}

	@GetMapping
	@Operation(summary = "발급 가능 쿠폰 목록 조회 API", description = "발급 가능한 쿠폰 목록을 조회하는 API입니다.")
	public ResponseEntity<BaseResponse<CouponPageResponseDto>> getCoupons(@LoginUser Long userId) {
		CouponPageResponseDto dto = couponService.getCoupons(userId);

		return BaseResponse.ok("발급가능한 쿠폰 조회 성공", dto, HttpStatus.OK);
	}
}
