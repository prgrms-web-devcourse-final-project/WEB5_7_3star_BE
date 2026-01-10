package com.threestar.trainus.domain.test.controller;

import com.threestar.trainus.domain.coupon.user.dto.CreateUserCouponResponseDto;
import com.threestar.trainus.domain.coupon.user.service.CouponService;
import com.threestar.trainus.domain.lesson.student.dto.LessonApplicationResponseDto;
import com.threestar.trainus.domain.lesson.student.service.StudentLessonService;
import com.threestar.trainus.domain.test.dto.TestRequestDto;
import com.threestar.trainus.domain.test.service.TestUserService;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.global.unit.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "동시성 테스트 API", description = "선착순 기능 테스트를 위한 API")
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestConcurrencyController {

	private final CouponService couponService;
	private final StudentLessonService studentLessonService;
	private final TestUserService testUserService;

	// 쿠폰 동시성 테스트
	@PostMapping("/coupons/{couponId}/no-lock")
	@Operation(summary = "쿠폰 발급 동시성 테스트 (락 미적용)", description = "쿠폰을 발급받는 테스트 API (락 미적용)")
	public ResponseEntity<BaseResponse<CreateUserCouponResponseDto>> issueCouponNoLock(
		@PathVariable Long couponId,
		@RequestBody TestRequestDto testRequestDto
	) {
		User user = testUserService.findOrCreateUser(testRequestDto.getUserId());
		CreateUserCouponResponseDto responseDto = couponService.createUserCouponWithoutLock(user.getId(), couponId);
		return BaseResponse.ok("쿠폰 발급 완료 (락 미적용)", responseDto, HttpStatus.CREATED);
	}

	@PostMapping("/coupons/{couponId}/pessimistic-lock")
	@Operation(summary = "쿠폰 발급 동시성 테스트 (비관적 락)", description = "쿠폰을 발급받는 테스트 API (비관적 락)")
	public ResponseEntity<BaseResponse<CreateUserCouponResponseDto>> issueCouponPessimisticLock(
		@PathVariable Long couponId,
		@RequestBody TestRequestDto testRequestDto
	) {
		User user = testUserService.findOrCreateUser(testRequestDto.getUserId());
		CreateUserCouponResponseDto responseDto = couponService.createUserCouponWithPessimisticLock(user.getId(), couponId);
		return BaseResponse.ok("쿠폰 발급 완료 (비관적 락)", responseDto, HttpStatus.CREATED);
	}

	@PostMapping("/coupons/{couponId}/distributed-lock")
	@Operation(summary = "쿠폰 발급 동시성 테스트 (분산 락)", description = "쿠폰을 발급받는 테스트 API (분산 락)")
	public ResponseEntity<BaseResponse<CreateUserCouponResponseDto>> issueCouponDistributedLock(
		@PathVariable Long couponId,
		@RequestBody TestRequestDto testRequestDto
	) {
		User user = testUserService.findOrCreateUser(testRequestDto.getUserId());
		CreateUserCouponResponseDto responseDto = couponService.createUserCouponWithDistributedLock(user.getId(), couponId);
		return BaseResponse.ok("쿠폰 발급 완료 (분산 락)", responseDto, HttpStatus.CREATED);
	}

	// 레슨 신청 동시성 테스트
	@PostMapping("/lessons/{lessonId}/application/no-lock")
	@Operation(summary = "레슨 신청 동시성 테스트 (락 미적용)", description = "레슨을 신청하는 테스트 API (락 미적용)")
	public ResponseEntity<BaseResponse<LessonApplicationResponseDto>> applyToLessonNoLock(
		@PathVariable Long lessonId,
		@RequestBody TestRequestDto testRequestDto
	) {
		User user = testUserService.findOrCreateUser(testRequestDto.getUserId());
		LessonApplicationResponseDto response = studentLessonService.applyToLessonWithoutLock(lessonId, user.getId());
		return BaseResponse.ok("레슨 신청 완료 (락 미적용)", response, HttpStatus.OK);
	}

	@PostMapping("/lessons/{lessonId}/application/pessimistic-lock")
	@Operation(summary = "레슨 신청 동시성 테스트 (비관적 락)", description = "레슨을 신청하는 테스트 API (비관적 락)")
	public ResponseEntity<BaseResponse<LessonApplicationResponseDto>> applyToLessonPessimisticLock(
		@PathVariable Long lessonId,
		@RequestBody TestRequestDto testRequestDto
	) {
		User user = testUserService.findOrCreateUser(testRequestDto.getUserId());
		LessonApplicationResponseDto response = studentLessonService.applyToLessonWithPessimisticLock(lessonId, user.getId());
		return BaseResponse.ok("레슨 신청 완료 (비관적 락)", response, HttpStatus.OK);
	}

	@PostMapping("/lessons/{lessonId}/application/distributed-lock")
	@Operation(summary = "레슨 신청 동시성 테스트 (분산 락)", description = "레슨을 신청하는 테스트 API (분산 락)")
	public ResponseEntity<BaseResponse<LessonApplicationResponseDto>> applyToLessonDistributedLock(
		@PathVariable Long lessonId,
		@RequestBody TestRequestDto testRequestDto
	) {
		User user = testUserService.findOrCreateUser(testRequestDto.getUserId());
		LessonApplicationResponseDto response = studentLessonService.applyToLessonWithDistributedLock(lessonId, user.getId());
		return BaseResponse.ok("레슨 신청 완료 (분산 락)", response, HttpStatus.OK);
	}
}
