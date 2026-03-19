package com.threestar.trainus.domain.test.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.coupon.issue.CouponIssueProducer;
import com.threestar.trainus.domain.coupon.user.dto.CreateUserCouponResponseDto;
import com.threestar.trainus.domain.coupon.user.service.CouponIssueFacade;
import com.threestar.trainus.domain.coupon.user.service.CouponService;
import com.threestar.trainus.domain.lesson.issue.LessonApplyProducer;
import com.threestar.trainus.domain.lesson.student.dto.LessonApplicationResponseDto;
import com.threestar.trainus.domain.lesson.student.service.StudentLessonFacade;
import com.threestar.trainus.domain.lesson.student.service.StudentLessonService;
import com.threestar.trainus.domain.lesson.teacher.service.AdminLessonService;
import com.threestar.trainus.domain.test.dto.TestRequestDto;
import com.threestar.trainus.domain.test.service.TestUserService;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.global.unit.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "동시성 테스트 API", description = "선착순 기능 테스트를 위한 API")
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestConcurrencyController {

	private final CouponService couponService;
	private final StudentLessonService studentLessonService;
	private final StudentLessonFacade studentLessonFacade;
	private final AdminLessonService adminLessonService;
	private final TestUserService testUserService;
	private final CouponIssueProducer couponIssueProducer;
	private final LessonApplyProducer lessonApplyProducer;
	private final CouponIssueFacade couponIssueFacade;

	@PostMapping("/users/create")
	@Operation(summary = "테스트 유저 대량 생성", description = "지정된 수만큼 테스트 유저를 미리 생성합니다.")
	public ResponseEntity<String> createUsers(@RequestParam int count) {
		testUserService.createUsers(count); // Todo setup 메서드로 전환
		return ResponseEntity.ok(count + "명의 테스트 유저 생성 완료");
	}

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
		CreateUserCouponResponseDto responseDto = couponService.createUserCouponWithPessimisticLock(user.getId(),
			couponId);
		return BaseResponse.ok("쿠폰 발급 완료 (비관적 락)", responseDto, HttpStatus.CREATED);
	}

	@PostMapping("/coupons/{couponId}/distributed-lock")
	@Operation(summary = "쿠폰 발급 동시성 테스트 (분산 락)", description = "쿠폰을 발급받는 테스트 API (분산 락)")
	public ResponseEntity<BaseResponse<CreateUserCouponResponseDto>> issueCouponDistributedLock(
		@PathVariable Long couponId,
		@RequestBody TestRequestDto testRequestDto
	) {
		User user = testUserService.findOrCreateUser(testRequestDto.getUserId());
		CreateUserCouponResponseDto responseDto = couponIssueFacade.issueCoupon(user.getId(),
			couponId);
		return BaseResponse.ok("쿠폰 발급 완료 (분산 락)", responseDto, HttpStatus.CREATED);
	}

	@PostMapping("/coupons/{couponId}/redis-stream")
	@Operation(summary = "쿠폰 발급 동시성 테스트 (메시지 큐)", description = "쿠폰을 발급받는 테스트 API (메시지 큐)")
	public ResponseEntity<?> issueCouponRedisStream(
		@PathVariable Long couponId,
		@RequestBody TestRequestDto testRequestDto
	) {
		User user = testUserService.findOrCreateUser2(testRequestDto.getUserId());
		boolean ok = couponIssueProducer.send(couponId, user.getId());
		if (!ok) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("sold out");
		}
		return ResponseEntity.accepted().build();
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
		LessonApplicationResponseDto response = studentLessonService.applyToLessonWithPessimisticLock(lessonId,
			user.getId());
		return BaseResponse.ok("레슨 신청 완료 (비관적 락)", response, HttpStatus.OK);
	}

	@PostMapping("/lessons/{lessonId}/application/distributed-lock")
	@Operation(summary = "레슨 신청 동시성 테스트 (분산 락)", description = "레슨을 신청하는 테스트 API (분산 락)")
	public ResponseEntity<BaseResponse<LessonApplicationResponseDto>> applyToLessonDistributedLock(
		@PathVariable Long lessonId,
		@RequestBody TestRequestDto testRequestDto
	) {
		User user = testUserService.findOrCreateUser(testRequestDto.getUserId());
		LessonApplicationResponseDto response = studentLessonFacade.applyToLessonWithDistributedLock(lessonId,
			user.getId());
		return BaseResponse.ok("레슨 신청 완료 (분산 락)", response, HttpStatus.OK);
	}

	@PostMapping("/lessons/{lessonId}/application/redis-stream")
	@Operation(summary = "레슨 신청 동시성 테스트 (메시지 큐)", description = "레슨을 신청하는 테스트 API (메시지 큐)")
	public ResponseEntity<?> applyToLessonRedisStream(
		@PathVariable Long lessonId,
		@RequestBody TestRequestDto testRequestDto
	) {
		String requestId = lessonApplyProducer.send(lessonId, testRequestDto.getUserId());
		if (requestId == null) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("sold out");
		}
		return ResponseEntity.accepted().body(requestId);
	}

	@PostMapping("/lessons/{lessonId}/stock/redis")
	@Operation(summary = "레슨 Redis 재고 세팅", description = "DB의 잔여 인원 정보를 Redis로 동기화합니다.")
	public ResponseEntity<Void> settingLessonRedisStock(@PathVariable Long lessonId) {
		adminLessonService.syncLessonStockToRedis(lessonId);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/reset")
	@Operation(summary = "모든 테스트 데이터 초기화", description = "DB와 Redis의 모든 테스트 데이터를 비우고 ID 시퀀스를 1로 리셋합니다.")
	public ResponseEntity<String> resetData() {
		testUserService.clearAllData();
		return ResponseEntity.ok("모든 테스트 데이터가 초기화되었습니다.");
	}

}
