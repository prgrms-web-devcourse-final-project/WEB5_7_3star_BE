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

	@PostMapping("/coupons/{couponId}")
	@Operation(summary = "쿠폰 발급 동시성 테스트", description = "쿠폰을 발급받는 테스트 API")
	public ResponseEntity<BaseResponse<CreateUserCouponResponseDto>> issueCouponForTest(
		@PathVariable Long couponId,
		@RequestBody TestRequestDto testRequestDto
	) {
		User user = testUserService.findOrCreateUser(testRequestDto.getUserId());
		CreateUserCouponResponseDto responseDto = couponService.createUserCoupon(user.getId(), couponId);
		return BaseResponse.ok("쿠폰 발급 완료", responseDto, HttpStatus.CREATED);
	}

	@PostMapping("/lessons/{lessonId}/application")
	@Operation(summary = "레슨 신청 동시성 테스트", description = "레슨을 신청하는 테스트 API")
	public ResponseEntity<BaseResponse<LessonApplicationResponseDto>> applyToLessonForTest(
		@PathVariable Long lessonId,
		@RequestBody TestRequestDto testRequestDto
	) {
		User user = testUserService.findOrCreateUser(testRequestDto.getUserId());
		LessonApplicationResponseDto response = studentLessonService.applyToLessonWithLock(lessonId, user.getId());
		return BaseResponse.ok("레슨 신청 완료", response, HttpStatus.OK);
	}
}
