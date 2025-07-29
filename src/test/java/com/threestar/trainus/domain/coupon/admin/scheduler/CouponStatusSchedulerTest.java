package com.threestar.trainus.domain.coupon.admin.scheduler;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;

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

@ExtendWith(MockitoExtension.class)
class CouponStatusSchedulerTest {
	@Mock
	private CouponRepository couponRepository;

	@Mock
	private UserCouponRepository userCouponRepository;

	@InjectMocks
	private CouponStatusScheduler couponStatusScheduler;

	@Test
	@DisplayName("비활성화된 쿠폰을 활성화하는 스케줄러 테스트")
	void updateCouponStatus_ActivateCoupons() {
		LocalDateTime now = LocalDateTime.now();

		Coupon inactiveCoupon = Coupon.builder()
			.name("테스트 쿠폰")
			.status(CouponStatus.INACTIVE)
			.category(CouponCategory.NORMAL)
			.openAt(now.minusHours(1)) // 1시간 전에 오픈
			.closeAt(now.plusDays(1))  // 내일 마감
			.build();

		given(couponRepository.findInactiveCouponsToActivate(any(LocalDateTime.class)))
			.willReturn(List.of(inactiveCoupon));
		given(couponRepository.findActiveCouponsToDeactivate(any(LocalDateTime.class)))
			.willReturn(List.of());

		couponStatusScheduler.updateCouponStatus();

		verify(couponRepository).findInactiveCouponsToActivate(any(LocalDateTime.class));
		verify(couponRepository).findActiveCouponsToDeactivate(any(LocalDateTime.class));
		verify(couponRepository).saveAll(anyList());
	}

	@Test
	@DisplayName("활성화된 쿠폰을 비활성화하는 스케줄러 테스트")
	void updateCouponStatus_DeactivateCoupons() {
		LocalDateTime now = LocalDateTime.now();

		Coupon activeCoupon = Coupon.builder()
			.name("테스트 쿠폰")
			.status(CouponStatus.ACTIVE)
			.category(CouponCategory.NORMAL)
			.openAt(now.minusDays(7))  // 7일 전에 오픈
			.closeAt(now.minusHours(1)) // 1시간 전에 마감
			.build();

		given(couponRepository.findInactiveCouponsToActivate(any(LocalDateTime.class)))
			.willReturn(List.of());
		given(couponRepository.findActiveCouponsToDeactivate(any(LocalDateTime.class)))
			.willReturn(List.of(activeCoupon));

		couponStatusScheduler.updateCouponStatus();

		verify(couponRepository).findInactiveCouponsToActivate(any(LocalDateTime.class));
		verify(couponRepository).findActiveCouponsToDeactivate(any(LocalDateTime.class));
		verify(couponRepository).saveAll(anyList());
	}

	@Test
	@DisplayName("만료된 유저쿠폰을 처리하는 스케줄러 테스트")
	void updateUserCouponStatus_ExpireCoupons() {
		LocalDateTime now = LocalDateTime.now();

		User user = User.builder().id(1L).build();
		Coupon coupon = Coupon.builder()
			.name("테스트 쿠폰")
			.status(CouponStatus.ACTIVE)
			.category(CouponCategory.NORMAL)
			.build();

		UserCoupon expiredUserCoupon = new UserCoupon(user, coupon, now.minusHours(1)); // 1시간 전에 만료

		given(userCouponRepository.findActiveUserCouponsToExpire(any(LocalDateTime.class)))
			.willReturn(List.of(expiredUserCoupon));

		couponStatusScheduler.updateUserCouponStatus();

		verify(userCouponRepository).findActiveUserCouponsToExpire(any(LocalDateTime.class));
		verify(userCouponRepository).saveAll(anyList());
	}

	@Test
	@DisplayName("처리할 쿠폰이 없는 경우 스케줄러 테스트")
	void updateCouponStatus_NoCouponsToProcess() {
		given(couponRepository.findInactiveCouponsToActivate(any(LocalDateTime.class)))
			.willReturn(List.of());
		given(couponRepository.findActiveCouponsToDeactivate(any(LocalDateTime.class)))
			.willReturn(List.of());

		couponStatusScheduler.updateCouponStatus();

		verify(couponRepository).findInactiveCouponsToActivate(any(LocalDateTime.class));
		verify(couponRepository).findActiveCouponsToDeactivate(any(LocalDateTime.class));
		verify(couponRepository, never()).saveAll(anyList());
	}
}
