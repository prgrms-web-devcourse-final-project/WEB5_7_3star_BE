package com.threestar.trainus.domain.coupon.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.coupon.admin.dto.CouponCreateRequestDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponCreateResponseDto;
import com.threestar.trainus.domain.coupon.admin.service.AdminCouponService;
import com.threestar.trainus.global.annotation.LoginUser;
import com.threestar.trainus.global.unit.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "관리자 쿠폰 API", description = "관리자 쿠폰 생성,수정 및 삭제 관련 API")
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

	private final AdminCouponService adminCouponService;

	@PostMapping
	@Operation(summary = "쿠폰 생성", description = "관리자가 새로운 쿠폰을 생성")
	public ResponseEntity<BaseResponse<CouponCreateResponseDto>> createCoupon(
		@Valid @RequestBody CouponCreateRequestDto request,
		@LoginUser Long loginUserId
	) {
		CouponCreateResponseDto response = adminCouponService.createCoupon(request, loginUserId);
		return BaseResponse.ok("쿠폰 생성이 완료되었습니다.", response, HttpStatus.OK);
	}
}
