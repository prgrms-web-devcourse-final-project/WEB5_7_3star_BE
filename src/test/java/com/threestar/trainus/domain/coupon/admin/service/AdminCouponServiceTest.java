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
}
