package com.threestar.trainus.domain.coupon.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.coupon.admin.dto.CouponCreateRequestDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponCreateResponseDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponDeleteResponseDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponDetailResponseDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponListResponseDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponListWrapperDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponUpdateRequestDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponUpdateResponseDto;
import com.threestar.trainus.domain.coupon.admin.mapper.AdminCouponMapper;
import com.threestar.trainus.domain.coupon.admin.service.AdminCouponService;
import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;
import com.threestar.trainus.global.annotation.LoginUser;
import com.threestar.trainus.global.dto.PageRequestDto;
import com.threestar.trainus.global.unit.BaseResponse;
import com.threestar.trainus.global.unit.PagedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "관리자 쿠폰 API", description = "관리자 쿠폰 생성,수정 및 삭제 관련 API")
@RestController
@RequestMapping("/api/v1/admin/coupons")
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

	@GetMapping
	@Operation(summary = "쿠폰 목록 조회", description = "관리자가 쿠폰 목록을 조회")
	public ResponseEntity<PagedResponse<CouponListWrapperDto>> getCoupons(
		@Valid @ModelAttribute PageRequestDto pageRequestDto,
		@RequestParam(required = false) CouponStatus status,
		@RequestParam(required = false) CouponCategory category,
		@LoginUser Long loginUserId
	) {
		CouponListResponseDto couponsInfo = adminCouponService.getCoupons(
			pageRequestDto.getPage(),
			pageRequestDto.getLimit(),
			status,
			category,
			loginUserId
		);

		CouponListWrapperDto coupons = AdminCouponMapper.toCouponListWrapperDto(couponsInfo);

		return PagedResponse.ok("쿠폰 목록 조회 완료.", coupons, couponsInfo.totalCount(), HttpStatus.OK);
	}

	@GetMapping("/{couponId}")
	@Operation(summary = "쿠폰 상세 조회", description = "관리자가 특정 쿠폰의 상세 정보를 조회")
	public ResponseEntity<BaseResponse<CouponDetailResponseDto>> getCouponDetail(
		@PathVariable Long couponId,
		@LoginUser Long loginUserId
	) {
		CouponDetailResponseDto response = adminCouponService.getCouponDetail(couponId, loginUserId);
		return BaseResponse.ok("쿠폰 상세 조회 완료", response, HttpStatus.OK);
	}

	@PatchMapping("/{couponId}")
	@Operation(summary = "쿠폰 수정", description = "관리자가 쿠폰 정보를 수정")
	public ResponseEntity<BaseResponse<CouponUpdateResponseDto>> updateCoupon(
		@PathVariable Long couponId,
		@Valid @RequestBody CouponUpdateRequestDto request,
		@LoginUser Long loginUserId
	) {
		CouponUpdateResponseDto response = adminCouponService.updateCoupon(couponId, request, loginUserId);
		return BaseResponse.ok("쿠폰 수정이 완료되었습니다.", response, HttpStatus.OK);
	}

	@DeleteMapping("/{couponId}")
	@Operation(summary = "쿠폰 삭제", description = "관리자가 쿠폰을 삭제")
	public ResponseEntity<BaseResponse<CouponDeleteResponseDto>> deleteCoupon(
		@PathVariable Long couponId,
		@LoginUser Long loginUserId
	) {
		CouponDeleteResponseDto response = adminCouponService.deleteCoupon(couponId, loginUserId);
		return BaseResponse.ok("쿠폰 삭제가 완료되었습니다.", response, HttpStatus.OK);
	}

}
