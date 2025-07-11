package com.threestar.trainus.coupon;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.threestar.trainus.domain.coupon.entity.Coupon;
import com.threestar.trainus.domain.coupon.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.entity.CouponStatus;
import com.threestar.trainus.domain.coupon.repository.CouponRepository;
import com.threestar.trainus.domain.coupon.repository.UserCouponRepository;
import com.threestar.trainus.domain.coupon.service.CouponService;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.exception.handler.BusinessException;

@SpringBootTest
@TestPropertySource(properties = {
	"spring.jpa.show-sql=false",
	"logging.level.org.hibernate.SQL=OFF",
	"logging.level.org.hibernate.type.descriptor.sql=OFF"
})
class CouponServiceTests {

	@Autowired
	CouponService couponService;

	@Autowired
	CouponRepository couponRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	UserCouponRepository userCouponRepository;

	List<User> users;
	Coupon coupon;

	@BeforeEach
	void setUp() {
		// 기존 데이터 정리
		userCouponRepository.deleteAll();
		couponRepository.deleteAll();
		userRepository.deleteAll();

		// 1000명의 유저 생성
		users = new ArrayList<>();
		for (int i = 0; i < 20000; i++) {
			User user = userRepository.save(
				User.builder()
					.email("user" + i + "@test.com")
					.password("test1234")
					.nickname("user" + i)
					.role(UserRole.USER)
					.build()
			);
			users.add(user);
		}

		// 재고 100장의 쿠폰 생성
		coupon = couponRepository.save(
			Coupon.builder()
				.name("선착순 쿠폰")
				.quantity(100)
				.category(CouponCategory.OPEN_RUN)
				.status(CouponStatus.ACTIVE)
				.discountPrice("1000")
				.minOrderPrice(20000)
				.expirationDate(LocalDateTime.now().plusDays(1))
				.openAt(LocalDateTime.now().minusMinutes(10))
				.closeAt(LocalDateTime.now().plusDays(1))
				.build()
		);

		// 데이터가 실제로 저장되었는지 확인
		System.out.println("사용자 수: " + userRepository.count());
		System.out.println("쿠폰 수: " + couponRepository.count());
		System.out.println("쿠폰 ID: " + coupon.getId());
		System.out.println("첫 번째 사용자 ID: " + users.get(0).getId());
	}

	@AfterEach
	void tearDown() {
		// 테스트 후 데이터 정리
		userCouponRepository.deleteAll();
		couponRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("1000명의 유저가 동시 요청해도 100장만 발급된다")
	void 쿠폰_동시_발급_테스트() throws InterruptedException {
		int threadCount = 20000;
		ExecutorService executorService = Executors.newFixedThreadPool(32);
		CountDownLatch latch = new CountDownLatch(threadCount);

		for (int i = 0; i < threadCount; i++) {
			final int idx = i;

			executorService.submit(() -> {
				try {
					couponService.createUserCoupon(users.get(idx).getId(), coupon.getId());
					System.out.println("Thread " + idx + " - 발급 성공");

				} catch (BusinessException e) {
					System.out.println("Thread " + idx + " - 예외: " + e.getErrorCode());

				} catch (Exception e) {
					System.out.println("Thread " + idx + " - 에러: " + e.getMessage());

				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executorService.shutdown();

		Long issuedCount = userCouponRepository.countByCouponId(coupon.getId());
		Integer leftQuantity = couponRepository.findById(coupon.getId()).get().getQuantity();
		System.out.println("총 발급된 쿠폰 수: " + issuedCount);
		System.out.println("쿠폰 남은 수량: " + leftQuantity);

		assertThat(issuedCount).isEqualTo(100L);
		assertThat(leftQuantity).isEqualTo(0);
	}

	@Test
	@DisplayName("단일 사용자 중복 발급 방지 테스트")
	void 중복_발급_방지_테스트() throws InterruptedException {
		int threadCount = 10;
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		CountDownLatch latch = new CountDownLatch(threadCount);

		// 첫 번째 사용자로 중복 발급 테스트
		User testUser = users.get(0);

		for (int i = 0; i < threadCount; i++) {
			final int idx = i;

			executorService.submit(() -> {
				try {
					couponService.createUserCoupon(testUser.getId(), coupon.getId());
					System.out.println("Thread " + idx + " - 발급 성공");

				} catch (BusinessException e) {
					System.out.println("Thread " + idx + " - 예외: " + e.getErrorCode());

				} catch (Exception e) {
					System.out.println("Thread " + idx + " - 에러: " + e.getMessage());

				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executorService.shutdown();

		Long issuedCount = userCouponRepository.countByUserIdAndCouponId(testUser.getId(), coupon.getId());

		System.out.println("해당 사용자에게 발급된 쿠폰 수: " + issuedCount);

		assertThat(issuedCount).isEqualTo(1L);
	}
}