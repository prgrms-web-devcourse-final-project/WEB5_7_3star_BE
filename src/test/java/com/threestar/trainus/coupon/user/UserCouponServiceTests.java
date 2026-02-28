package com.threestar.trainus.coupon.user;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;

import com.threestar.trainus.domain.coupon.user.dto.CouponPageResponseDto;
import com.threestar.trainus.domain.coupon.user.dto.CouponResponseDto;
import com.threestar.trainus.domain.coupon.user.dto.CreateUserCouponResponseDto;
import com.threestar.trainus.domain.coupon.user.dto.UserCouponPageResponseDto;
import com.threestar.trainus.domain.coupon.user.entity.Coupon;
import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;
import com.threestar.trainus.domain.coupon.user.entity.UserCoupon;
import com.threestar.trainus.domain.coupon.user.repository.CouponRepository;
import com.threestar.trainus.domain.coupon.user.repository.UserCouponRepository;
import com.threestar.trainus.domain.coupon.user.service.CouponService;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.config.JpaAuditingConfig;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

@ExtendWith(MockitoExtension.class)
@Import(JpaAuditingConfig.class)
public class UserCouponServiceTests {

	@Mock
	private UserCouponRepository userCouponRepository;

	@Mock
	private CouponRepository couponRepository;

	@Mock
	private UserService userService;

	@InjectMocks
	private CouponService couponService;

	@Test
	void createUserCouponWithDistributedLock_정상_발급() {
		Long userId = 1L;
		Long couponId = 2L;

		User user = createMockUser();

		Coupon coupon = Coupon.builder()
			.id(couponId)
			.category(CouponCategory.NORMAL)
			.closeAt(LocalDateTime.now().plusDays(1))
			.expirationDate(LocalDateTime.now().plusDays(30))
			.openAt(LocalDateTime.now())
			.build();

		given(userService.getUserById(userId))
			.willReturn(user);
		given(couponRepository.findById(couponId))
			.willReturn(Optional.of(coupon));
		given(userCouponRepository.existsByUserIdAndCouponId(userId, couponId))
			.willReturn(false);

		CreateUserCouponResponseDto responseDto = couponService.issueCoupon(userId, couponId);

		assertThat(responseDto).isNotNull();
		assertThat(responseDto.couponId()).isEqualTo(couponId);
		then(userCouponRepository).should().save(any(UserCoupon.class));

	}

	@Test
	void createUserCouponWithDistributedLock_종료시각_지나면_예외처리() {
		Long userId = 1L;
		Long couponId = 2L;

		User user = createMockUser();

		Coupon coupon = Coupon.builder()
			.id(couponId)
			.category(CouponCategory.NORMAL)
			.closeAt(LocalDateTime.now().minusDays(1))
			.expirationDate(LocalDateTime.now().plusDays(30))
			.build();

		given(userService.getUserById(userId))
			.willReturn(user);
		given(couponRepository.findById(couponId))
			.willReturn(Optional.of(coupon));

		assertThatThrownBy(() -> couponService.issueCoupon(userId, couponId))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(ErrorCode.COUPON_EXPIRED.getMessage());
	}

	@Test
	void createUserCouponWithDistributedLock_중복발급_예외처리() {
		Long userId = 1L;
		Long couponId = 2L;

		User user = createMockUser();

		Coupon coupon = Coupon.builder()
			.id(couponId)
			.category(CouponCategory.NORMAL)
			.closeAt(LocalDateTime.now().plusDays(1))
			.expirationDate(LocalDateTime.now().plusDays(30))
			.build();

		given(userService.getUserById(userId))
			.willReturn(user);
		given(couponRepository.findById(couponId))
			.willReturn(Optional.of(coupon));
		given(userCouponRepository.existsByUserIdAndCouponId(userId, couponId))
			.willReturn(true);

		assertThatThrownBy(() -> couponService.issueCoupon(userId, couponId))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(ErrorCode.COUPON_ALREADY_ISSUED.getMessage());

	}

	@Test
	void createUserCouponWithDistributedLock_선착순쿠폰_오픈전_예외처리() {
		Long userId = 1L;
		Long couponId = 2L;

		User user = createMockUser();

		Coupon coupon = Coupon.builder()
			.id(couponId)
			.openAt(LocalDateTime.now().plusDays(1))
			.category(CouponCategory.OPEN_RUN)
			.closeAt(LocalDateTime.now().plusDays(1))
			.expirationDate(LocalDateTime.now().plusDays(30))
			.build();

		given(userService.getUserById(userId))
			.willReturn(user);
		given(couponRepository.findById(couponId))
			.willReturn(Optional.of(coupon));

		assertThatThrownBy(() -> couponService.issueCoupon(userId, couponId))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(ErrorCode.COUPON_NOT_YET_OPEN.getMessage());
	}

	@Test
	void createUserCouponWithDistributedLock_수량소진_예외() {
		Long userId = 1L;
		Long couponId = 2L;

		User user = createMockUser();

		Coupon coupon = Coupon.builder()
			.id(couponId)
			.quantity(0)
			.openAt(LocalDateTime.now().minusDays(1))
			.category(CouponCategory.OPEN_RUN)
			.closeAt(LocalDateTime.now().plusDays(1))
			.expirationDate(LocalDateTime.now().plusDays(30))
			.build();

		given(userService.getUserById(userId))
			.willReturn(user);
		given(couponRepository.findById(couponId))
			.willReturn(Optional.of(coupon));
		assertThatThrownBy(() -> couponService.issueCoupon(userId, couponId))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(ErrorCode.COUPON_BE_EXHAUSTED.getMessage());
	}

	@Test
	void getUserCoupons_상태없는_성공케이스() {
		Long userId = 1L;
		User user = createMockUser();

		Coupon coupon = Coupon.builder()
			.id(1L)
			.name("10% 할인 쿠폰")
			.discountPrice("10%")
			.minOrderPrice(10000)
			.expirationDate(LocalDateTime.now().plusDays(7))
			.quantity(100)
			.category(CouponCategory.NORMAL)
			.openAt(LocalDateTime.now().minusDays(1))
			.closeAt(LocalDateTime.now().plusDays(3))
			.build();

		UserCoupon userCoupon = new UserCoupon(user, coupon, coupon.getExpirationDate());

		willDoNothing().given(userService).validateUserExists(userId);

		given(userCouponRepository.findAllByUserIdWithCoupon(userId))
			.willReturn(List.of(userCoupon));

		UserCouponPageResponseDto dto = couponService.getUserCoupons(userId, null);

		assertThat(dto).isNotNull();
		assertThat(dto.userCoupons()).hasSize(1);
	}

	@Test
	void getUserCoupons_상태있는_성공케이스() {
		Long userId = 1L;
		CouponStatus status = CouponStatus.INACTIVE;
		User user = createMockUser();

		Coupon coupon = Coupon.builder()
			.id(1L)
			.name("10% 할인 쿠폰")
			.discountPrice("10%")
			.minOrderPrice(10000)
			.expirationDate(LocalDateTime.now().plusDays(7))
			.quantity(100)
			.category(CouponCategory.NORMAL)
			.openAt(LocalDateTime.now().minusDays(1))
			.closeAt(LocalDateTime.now().plusDays(3))
			.status(CouponStatus.INACTIVE)
			.build();

		Coupon coupon2 = Coupon.builder()
			.id(2L)
			.name("10% 할인 쿠폰")
			.discountPrice("10%")
			.minOrderPrice(10000)
			.expirationDate(LocalDateTime.now().plusDays(7))
			.quantity(100)
			.category(CouponCategory.NORMAL)
			.openAt(LocalDateTime.now().minusDays(1))
			.closeAt(LocalDateTime.now().plusDays(3))
			.status(CouponStatus.ACTIVE)
			.build();

		UserCoupon userCoupon = new UserCoupon(user, coupon, coupon.getExpirationDate());
		willDoNothing().given(userService).validateUserExists(userId);
		given(userCouponRepository.findAllByUserIdAndStatusWithCoupon(userId, status))
			.willReturn(List.of(userCoupon));

		UserCouponPageResponseDto dto = couponService.getUserCoupons(userId, status);

		assertThat(dto).isNotNull();
		assertThat(dto.userCoupons()).hasSize(1);
		assertThat(dto.userCoupons().get(0).couponId()).isEqualTo(coupon.getId());
	}

	@Test
	void getCoupons_성공_테스트() {
		Long userId = 1L;

		List<CouponResponseDto> dummyDtoList = List.of(mock(CouponResponseDto.class));
		willDoNothing().given(userService).validateUserExists(userId);
		given(couponRepository.findAvailableCouponsWithOwnership(eq(userId), any())).willReturn(dummyDtoList);

		// when
		CouponPageResponseDto result = couponService.getCoupons(userId);

		// then
		assertThat(result).isNotNull();
		assertThat(result.coupons()).hasSize(1);
	}

	private User createMockUser() {
		return User.builder()
			.email("user@test.com")
			.password("test1234")
			.nickname("user")
			.role(UserRole.USER)
			.build();
	}
}
