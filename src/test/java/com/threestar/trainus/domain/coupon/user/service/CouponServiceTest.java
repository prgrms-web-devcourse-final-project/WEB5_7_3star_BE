package com.threestar.trainus.domain.coupon.user.service;

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

import com.threestar.trainus.domain.coupon.user.entity.Coupon;
import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;
import com.threestar.trainus.domain.coupon.user.entity.UserCoupon;
import com.threestar.trainus.domain.coupon.user.repository.CouponRepository;
import com.threestar.trainus.domain.coupon.user.repository.UserCouponRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

	@Mock
	private CouponRepository couponRepository;

	@Mock
	private UserCouponRepository userCouponRepository;

	@Mock
	private UserService userService;

	@InjectMocks
	private CouponService couponService;

	@Test
	@DisplayName("퍼센트 할인 쿠폰은 원가 기준 할인 금액을 계산한다")
	void calculateDiscountedPrice_Percentage() {
		UserCoupon userCoupon = new UserCoupon(createUser(1L), createCoupon("30%"), LocalDateTime.now().plusDays(10));

		int discount = couponService.calculateDiscountedPrice(50000, userCoupon);

		assertThat(discount).isEqualTo(15000);
	}

	@Test
	@DisplayName("정액 할인 쿠폰은 원 단위 문자열에서 할인 금액을 계산한다")
	void calculateDiscountedPrice_FixedAmount() {
		UserCoupon userCoupon = new UserCoupon(createUser(1L), createCoupon("5000원"), LocalDateTime.now().plusDays(10));

		int discount = couponService.calculateDiscountedPrice(50000, userCoupon);

		assertThat(discount).isEqualTo(5000);
	}

	@Test
	@DisplayName("사용자와 쿠폰 ID로 조회한 ACTIVE 쿠폰만 유효 쿠폰으로 반환한다")
	void getValidUserCoupon_Active() {
		Long userId = 1L;
		Long couponId = 10L;
		UserCoupon userCoupon = new UserCoupon(createUser(userId), createCoupon("5000원"), LocalDateTime.now().plusDays(10));
		given(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).willReturn(Optional.of(userCoupon));

		UserCoupon result = couponService.getValidUserCoupon(couponId, userId);

		assertThat(result).isSameAs(userCoupon);
		verify(userCouponRepository).findByUserIdAndCouponId(userId, couponId);
	}

	@Test
	@DisplayName("사용자와 쿠폰 ID로 조회한 INACTIVE 쿠폰은 유효 쿠폰 조회시 예외 처리한다")
	void getValidUserCoupon_InactiveThrows() {
		Long userId = 1L;
		Long couponId = 10L;
		UserCoupon userCoupon = new UserCoupon(createUser(userId), createCoupon("5000원"), LocalDateTime.now().plusDays(10));
		userCoupon.use();
		given(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).willReturn(Optional.of(userCoupon));

		assertThatThrownBy(() -> couponService.getValidUserCoupon(couponId, userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.COUPON_NOT_FOUND);
		verify(userCouponRepository).findByUserIdAndCouponId(userId, couponId);
	}

	@Test
	@DisplayName("ACTIVE 쿠폰 사용시 상태를 INACTIVE로 바꾸고 저장한다")
	void useCoupon_ActiveSaves() {
		UserCoupon userCoupon = new UserCoupon(createUser(1L), createCoupon("5000원"), LocalDateTime.now().plusDays(10));

		couponService.useCoupon(userCoupon);

		assertThat(userCoupon.getStatus()).isEqualTo(CouponStatus.INACTIVE);
		verify(userCouponRepository).save(userCoupon);
	}

	@Test
	@DisplayName("INACTIVE 쿠폰 사용 요청은 저장하지 않는다")
	void useCoupon_InactiveDoesNothing() {
		UserCoupon userCoupon = new UserCoupon(createUser(1L), createCoupon("5000원"), LocalDateTime.now().plusDays(10));
		userCoupon.use();

		couponService.useCoupon(userCoupon);

		assertThat(userCoupon.getStatus()).isEqualTo(CouponStatus.INACTIVE);
		verify(userCouponRepository, never()).save(any(UserCoupon.class));
	}

	@Test
	@DisplayName("INACTIVE 쿠폰 복원시 ACTIVE로 바꾸고 저장한다")
	void restoreCoupon_InactiveSaves() {
		UserCoupon userCoupon = new UserCoupon(createUser(1L), createCoupon("5000원"), LocalDateTime.now().plusDays(10));
		userCoupon.use();

		couponService.restoreCoupon(userCoupon);

		assertThat(userCoupon.getStatus()).isEqualTo(CouponStatus.ACTIVE);
		assertThat(userCoupon.getUseDate()).isNull();
		verify(userCouponRepository).save(userCoupon);
	}

	@Test
	@DisplayName("ACTIVE 쿠폰 복원 요청은 저장하지 않는다")
	void restoreCoupon_ActiveDoesNothing() {
		UserCoupon userCoupon = new UserCoupon(createUser(1L), createCoupon("5000원"), LocalDateTime.now().plusDays(10));

		couponService.restoreCoupon(userCoupon);

		assertThat(userCoupon.getStatus()).isEqualTo(CouponStatus.ACTIVE);
		verify(userCouponRepository, never()).save(any(UserCoupon.class));
	}

	private Coupon createCoupon(String discountPrice) {
		LocalDateTime now = LocalDateTime.now();
		return Coupon.builder()
			.name("테스트 쿠폰")
			.discountPrice(discountPrice)
			.minOrderPrice(10000)
			.status(CouponStatus.ACTIVE)
			.quantity(100)
			.category(CouponCategory.NORMAL)
			.openAt(now.minusDays(1))
			.closeAt(now.plusDays(1))
			.expirationDate(now.plusDays(30))
			.build();
	}

	private User createUser(Long userId) {
		return User.builder()
			.id(userId)
			.email("test" + userId + "@test.com")
			.password("encoded")
			.nickname("테스트유저" + userId)
			.role(UserRole.USER)
			.build();
	}
}
