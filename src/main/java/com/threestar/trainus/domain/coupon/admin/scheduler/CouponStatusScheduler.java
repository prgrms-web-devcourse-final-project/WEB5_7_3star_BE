package com.threestar.trainus.domain.coupon.admin.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.coupon.user.entity.Coupon;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;
import com.threestar.trainus.domain.coupon.user.entity.UserCoupon;
import com.threestar.trainus.domain.coupon.user.repository.CouponRepository;
import com.threestar.trainus.domain.coupon.user.repository.UserCouponRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponStatusScheduler {

	private final CouponRepository couponRepository;
	private final UserCouponRepository userCouponRepository;

	/**
	 * 1분마다 쿠폰 상태를 확인하여 업데이트를 진행
	 * - INACTIVE → ACTIVE (오픈 시간 도달)
	 * - ACTIVE → INACTIVE (마감 시간 도달)
	 */
	@Scheduled(cron = "0 * * * * *") // 1분마다 실행
	@Transactional
	public void updateCouponStatus() {
		LocalDateTime now = LocalDateTime.now();

		try {
			// 비활성화 → 활성화(오픈시간에 도달한 쿠폰들)
			activateExpiredCoupons(now);

			// 활성화 → 비활성화(마감시간에 도달한 쿠폰들)
			deactivateExpiredCoupons(now);

		} catch (Exception e) {
			log.error("쿠폰 상태 업데이트 중 오류 발생", e);
		}
	}

	/**
	 * 유저쿠폰의 만료 상태를 확인하여 업데이트를 진행
	 * - ACTIVE → INACTIVE (유효기간 종료)
	 */
	@Scheduled(cron = "0 * * * * *") // 1분 마다 실행
	@Transactional
	public void updateUserCouponStatus() {
		LocalDateTime now = LocalDateTime.now();

		try {
			expireUserCoupons(now);
		} catch (Exception e) {
			log.error("유저쿠폰 상태 업데이트 중 오류 발생", e);
		}
	}

	private void activateExpiredCoupons(LocalDateTime now) {
		// 오픈 시간지났지만 아직 비활성화 상태인 쿠폰들 조회
		List<Coupon> couponsToActivate = couponRepository.findInactiveCouponsToActivate(now);

		if (!couponsToActivate.isEmpty()) {
			for (Coupon coupon : couponsToActivate) {
				coupon.updateStatus(CouponStatus.ACTIVE);
				log.info("쿠폰 활성화: ID={}, 이름={}", coupon.getId(), coupon.getName());
			}

			couponRepository.saveAll(couponsToActivate);
			log.info("총 {}개의 쿠폰이 활성화되었습니다.", couponsToActivate.size());
		}
	}

	private void deactivateExpiredCoupons(LocalDateTime now) {
		// 마감 시간이 지났지만 아직 활성화 상태인 쿠폰들 조회
		List<Coupon> couponsToDeactivate = couponRepository.findActiveCouponsToDeactivate(now);

		if (!couponsToDeactivate.isEmpty()) {
			for (Coupon coupon : couponsToDeactivate) {
				coupon.updateStatus(CouponStatus.INACTIVE);
				log.info("쿠폰 비활성화: ID={}, 이름={}", coupon.getId(), coupon.getName());
			}

			couponRepository.saveAll(couponsToDeactivate);
			log.info("총 {}개의 쿠폰이 비활성화되었습니다.", couponsToDeactivate.size());
		}
	}

	private void expireUserCoupons(LocalDateTime now) {
		// 유효기간이 지났지만 아직 활성화 상태인 유저쿠폰들 조회
		List<UserCoupon> userCouponsToExpire = userCouponRepository.findActiveUserCouponsToExpire(now);

		if (!userCouponsToExpire.isEmpty()) {
			for (UserCoupon userCoupon : userCouponsToExpire) {
				userCoupon.use(); // 만료 처리
				log.info("유저쿠폰 만료: UserID={}, CouponID={}",
					userCoupon.getUser().getId(), userCoupon.getCoupon().getId());
			}

			userCouponRepository.saveAll(userCouponsToExpire);
			log.info("총 {}개의 유저쿠폰이 만료되었습니다.", userCouponsToExpire.size());
		}
	}
}
