package com.threestar.trainus.domain.coupon.user.concurrency;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import com.threestar.trainus.domain.coupon.user.entity.Coupon;
import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;
import com.threestar.trainus.domain.coupon.user.repository.CouponRepository;
import com.threestar.trainus.domain.coupon.user.repository.UserCouponRepository;
import com.threestar.trainus.domain.coupon.user.service.CouponIssueFacade;
import com.threestar.trainus.domain.coupon.user.service.CouponService;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
public class UserCouponDistributedLockIntegrationTest {

	@Autowired
	CouponIssueFacade couponIssueFacade;

	@Autowired
	CouponRepository couponRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	UserCouponRepository userCouponRepository;

	private static final int COUPON_QUANTITY = 300;
	private static final int CONCURRENT_USERS = 3000;
	List<User> users;
	Coupon coupon;

	@BeforeEach
	void setUp() {
		users = new ArrayList<>();
		for (int i = 0; i < CONCURRENT_USERS; i++) {
			User user = userRepository.save(
				User.builder()
					.email("user" + i + "@test.com")
					.password("12341234")
					.nickname("user" + i)
					.role(UserRole.USER)
					.build()
			);
			users.add(user);
			userRepository.flush();
		}

		// 쿠폰 생성
		coupon = couponRepository.save(
			Coupon.builder()
				.name("Redis 분산락 쿠폰")
				.quantity(COUPON_QUANTITY)
				.category(CouponCategory.OPEN_RUN)
				.status(CouponStatus.ACTIVE)
				.discountPrice("1000")
				.minOrderPrice(20000)
				.expirationDate(LocalDateTime.now().plusDays(1))
				.openAt(LocalDateTime.now().minusMinutes(10))
				.closeAt(LocalDateTime.now().plusDays(1))
				.build()
		);
	}

	@AfterEach
	void tearDown() {
		userCouponRepository.deleteAll();
		couponRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("동시 요청 시 - Redis 분산락 적용: 발급된 쿠폰 수량만큼 발급")
	void 쿠폰_동시_발급_테스트() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(100);
		CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);

		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failCount = new AtomicInteger();

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		for (int i = 0; i < CONCURRENT_USERS; i++) {
			final int idx = i;

			executor.submit(() -> {
				try {
					couponIssueFacade.issueCoupon(users.get(idx).getId(), coupon.getId());
					log.info("[성공] userId: {} | 현재 발급 수: {}", idx,
						userCouponRepository.countByCouponId(coupon.getId()));
					successCount.incrementAndGet();
				} catch (Exception e) {
					failCount.incrementAndGet();
					log.error("[실패] userId: {} | message: {}", idx, e.getMessage());
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();
		executor.shutdown();
		stopWatch.stop();

		long issuedCount = userCouponRepository.countByCouponId(coupon.getId());
		int remainingQuantity = couponRepository.findById(coupon.getId()).orElseThrow().getQuantity();

		log.info("총 소요 시간(ms): {}", stopWatch.getTotalTimeMillis());
		log.info("총 요청 수: {}", CONCURRENT_USERS);
		log.info("성공 요청 수: {}", successCount.get());
		log.info("실패 요청 수: {}", failCount.get());
		log.info("DB 기준 발급 수(userCoupon): {}", issuedCount);
		log.info("남은 수량: {}", remainingQuantity);

		Assertions.assertEquals(COUPON_QUANTITY, issuedCount, "정확한 수량만큼 발급돼야 함");
		Assertions.assertEquals(successCount.get(), COUPON_QUANTITY, "성공 요청 수도 수량과 같아야 함");
	}
}
