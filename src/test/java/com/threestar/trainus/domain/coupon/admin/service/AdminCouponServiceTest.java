package com.threestar.trainus.domain.coupon.admin.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threestar.trainus.domain.coupon.admin.dto.CouponCreateRequestDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponCreateResponseDto;
import com.threestar.trainus.domain.coupon.user.entity.Coupon;
import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;
import com.threestar.trainus.domain.coupon.user.repository.CouponRepository;
import com.threestar.trainus.domain.coupon.user.repository.UserCouponRepository;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

@ExtendWith(MockitoExtension.class)
class AdminCouponServiceTest {

	@Mock
	private CouponRepository couponRepository;

	@Mock
	private UserCouponRepository userCouponRepository;

	@Mock
	private UserService userService;

	@InjectMocks
	private AdminCouponService adminCouponService;

	@Test
	@DisplayName("정상적인 쿠폰 생성 테스트")
	void createCoupon_Success() {
		Long userId = 1L;
		LocalDateTime now = LocalDateTime.now();

		CouponCreateRequestDto request = new CouponCreateRequestDto(
			"테스트 쿠폰",
			now.plusDays(30),
			"5000",
			10000,
			CouponStatus.ACTIVE,
			100,
			CouponCategory.NORMAL,
			now,
			now.plusDays(7)
		);

		Coupon savedCoupon = Coupon.builder()
			.name("테스트 쿠폰")
			.expirationDate(now.plusDays(30))
			.discountPrice("5000")
			.minOrderPrice(10000)
			.status(CouponStatus.ACTIVE)
			.quantity(100)
			.category(CouponCategory.NORMAL)
			.openAt(now)
			.closeAt(now.plusDays(7))
			.build();

		given(couponRepository.save(any(Coupon.class))).willReturn(savedCoupon);

		CouponCreateResponseDto response = adminCouponService.createCoupon(request, userId);

		assertThat(response).isNotNull();
		assertThat(response.couponName()).isEqualTo("테스트 쿠폰");
		verify(userService).validateAdminRole(userId);
		verify(couponRepository).save(any(Coupon.class));
	}

	@Test
	@DisplayName("할인가격 형식이 잘못된 경우 예외 발생")
	void createCoupon_InvalidDiscountPrice() {
		Long userId = 1L;
		LocalDateTime now = LocalDateTime.now();

		CouponCreateRequestDto request = new CouponCreateRequestDto(
			"테스트 쿠폰",
			now.plusDays(30),
			"잘못된형식", // 잘못된 할인가격
			10000,
			CouponStatus.ACTIVE,
			100,
			CouponCategory.NORMAL,
			now,
			now.plusDays(7)
		);

		assertThatThrownBy(() -> adminCouponService.createCoupon(request, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
	}

	@Test
	@DisplayName("쿠폰 상세 조회 성공")
	void getCouponDetail_Success() {
		Long couponId = 1L;
		Long userId = 1L;

		Coupon coupon = Coupon.builder()
			.name("테스트 쿠폰")
			.discountPrice("5000")
			.minOrderPrice(10000)
			.status(CouponStatus.ACTIVE)
			.quantity(100)
			.category(CouponCategory.NORMAL)
			.build();

		given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
		given(userCouponRepository.countByCouponId(couponId)).willReturn(10L);

		var response = adminCouponService.getCouponDetail(couponId, userId);

		assertThat(response).isNotNull();
		assertThat(response.couponName()).isEqualTo("테스트 쿠폰");
		assertThat(response.issuedCount()).isEqualTo(10);
		verify(userService).validateAdminRole(userId);
	}

	@Test
	@DisplayName("존재하지 않는 쿠폰 조회시 예외 발생")
	void getCouponDetail_NotFound() {
		Long couponId = 999L;
		Long userId = 1L;

		given(couponRepository.findById(couponId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> adminCouponService.getCouponDetail(couponId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
	}

	@Test
	@DisplayName("쿠폰 삭제 성공")
	void deleteCoupon_Success() {
		Long couponId = 1L;
		Long userId = 1L;

		Coupon coupon = Coupon.builder()
			.name("테스트 쿠폰")
			.status(CouponStatus.ACTIVE)
			.build();

		given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
		given(userCouponRepository.countByCouponId(couponId)).willReturn(0L);
		given(couponRepository.save(any(Coupon.class))).willReturn(coupon);

		var response = adminCouponService.deleteCoupon(couponId, userId);

		assertThat(response).isNotNull();
		assertThat(response.couponName()).isEqualTo("테스트 쿠폰");
		verify(userService).validateAdminRole(userId);
		verify(couponRepository).save(coupon);
	}

	@Test
	@DisplayName("이미 삭제된 쿠폰 삭제시 예외 발생")
	void deleteCoupon_AlreadyDeleted() {
		Long couponId = 1L;
		Long userId = 1L;

		Coupon coupon = Coupon.builder()
			.name("테스트 쿠폰")
			.status(CouponStatus.ACTIVE)
			.build();

		coupon.markAsDeleted();

		given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

		assertThatThrownBy(() -> adminCouponService.deleteCoupon(couponId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);
	}

	@Test
	@DisplayName("발급되지 않은 쿠폰 삭제 성공")
	void deleteCoupon_Success_NoIssuedCoupons() {
		Long couponId = 1L;
		Long userId = 1L;

		Coupon coupon = Coupon.builder()
			.name("테스트 쿠폰")
			.status(CouponStatus.ACTIVE)
			.discountPrice("5000")
			.minOrderPrice(10000)
			.category(CouponCategory.NORMAL)
			.build();

		given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
		given(userCouponRepository.countByCouponId(couponId)).willReturn(0L); // 발급된 쿠폰 없음
		given(couponRepository.save(any(Coupon.class))).willReturn(coupon);

		var response = adminCouponService.deleteCoupon(couponId, userId);

		assertThat(response).isNotNull();
		assertThat(response.couponName()).isEqualTo("테스트 쿠폰");
		verify(userService).validateAdminRole(userId);
		verify(couponRepository).save(coupon);
		assertThat(coupon.isDeleted()).isTrue(); // 삭제 상태 확인
	}

	@Test
	@DisplayName("발급된 쿠폰이 있는 경우 삭제 실패")
	void deleteCoupon_Fail_HasIssuedCoupons() {
		Long couponId = 1L;
		Long userId = 1L;

		Coupon coupon = Coupon.builder()
			.name("테스트 쿠폰")
			.status(CouponStatus.ACTIVE)
			.discountPrice("5000")
			.minOrderPrice(10000)
			.category(CouponCategory.NORMAL)
			.build();

		given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
		given(userCouponRepository.countByCouponId(couponId)).willReturn(5L); // 5명이 발급받음

		assertThatThrownBy(() -> adminCouponService.deleteCoupon(couponId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_CANNOT_DELETE_ISSUED);

		verify(userService).validateAdminRole(userId);
		verify(couponRepository, never()).save(any(Coupon.class)); // 저장되지 않음
		assertThat(coupon.isDeleted()).isFalse(); // 삭제되지 않음
	}

	@Test
	@DisplayName("이미 삭제된 쿠폰 삭제 시도 시 실패")
	void deleteCoupon_Fail_AlreadyDeleted() {
		Long couponId = 1L;
		Long userId = 1L;

		Coupon coupon = Coupon.builder()
			.name("테스트 쿠폰")
			.status(CouponStatus.ACTIVE)
			.discountPrice("5000")
			.minOrderPrice(10000)
			.category(CouponCategory.NORMAL)
			.build();

		coupon.markAsDeleted(); // 미리 삭제 상태로 만듦

		given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

		assertThatThrownBy(() -> adminCouponService.deleteCoupon(couponId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);

		verify(userService).validateAdminRole(userId);
		verify(userCouponRepository, never()).countByCouponId(anyLong()); // 발급 수량 조회하지 않음
	}

	@Test
	@DisplayName("존재하지 않는 쿠폰 삭제 시도 시 실패")
	void deleteCoupon_Fail_CouponNotFound() {
		Long couponId = 999L;
		Long userId = 1L;

		given(couponRepository.findById(couponId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> adminCouponService.deleteCoupon(couponId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST_DATA);

		verify(userService).validateAdminRole(userId);
		verify(userCouponRepository, never()).countByCouponId(anyLong());
	}

	@Test
	@DisplayName("선착순 쿠폰 - 발급되지 않은 경우 삭제 성공")
	void deleteCoupon_Success_OpenRunCoupon_NoIssued() {
		Long couponId = 1L;
		Long userId = 1L;

		Coupon openRunCoupon = Coupon.builder()
			.name("선착순 쿠폰")
			.status(CouponStatus.ACTIVE)
			.discountPrice("10%")
			.minOrderPrice(50000)
			.quantity(100)
			.category(CouponCategory.OPEN_RUN)
			.build();

		given(couponRepository.findById(couponId)).willReturn(Optional.of(openRunCoupon));
		given(userCouponRepository.countByCouponId(couponId)).willReturn(0L); // 아무도 발급받지 않음
		given(couponRepository.save(any(Coupon.class))).willReturn(openRunCoupon);

		var response = adminCouponService.deleteCoupon(couponId, userId);

		assertThat(response).isNotNull();
		assertThat(response.couponName()).isEqualTo("선착순 쿠폰");
		verify(couponRepository).save(openRunCoupon);
		assertThat(openRunCoupon.isDeleted()).isTrue();
	}

	@Test
	@DisplayName("선착순 쿠폰 - 발급된 경우 삭제 실패")
	void deleteCoupon_Fail_OpenRunCoupon_HasIssued() {
		Long couponId = 1L;
		Long userId = 1L;

		Coupon openRunCoupon = Coupon.builder()
			.name("선착순 쿠폰")
			.status(CouponStatus.ACTIVE)
			.discountPrice("10%")
			.minOrderPrice(50000)
			.quantity(100)
			.category(CouponCategory.OPEN_RUN)
			.build();

		given(couponRepository.findById(couponId)).willReturn(Optional.of(openRunCoupon));
		given(userCouponRepository.countByCouponId(couponId)).willReturn(50L); // 50명이 발급받음

		assertThatThrownBy(() -> adminCouponService.deleteCoupon(couponId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_CANNOT_DELETE_ISSUED);

		verify(couponRepository, never()).save(any(Coupon.class));
		assertThat(openRunCoupon.isDeleted()).isFalse();
	}
}
