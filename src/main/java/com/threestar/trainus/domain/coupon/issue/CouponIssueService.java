package com.threestar.trainus.domain.coupon.issue;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.coupon.user.entity.Coupon;
import com.threestar.trainus.domain.coupon.user.entity.UserCoupon;
import com.threestar.trainus.domain.coupon.user.repository.CouponRepository;
import com.threestar.trainus.domain.coupon.user.repository.UserCouponRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueService {
	private final CouponRepository couponRepository;
	private final UserCouponRepository userCouponRepository;
	private final UserService userService;

	@Transactional
	public boolean issue(Long couponId, Long userId) {
		try {
			// 멱등성 검증 (중복 신청 확인)
			if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
				log.warn("Coupon issue failed: Duplicate application. couponId={}, userId={}", couponId, userId);
				return true;
			}

			User user = userService.getUserById(userId);
			Coupon coupon = couponRepository.findById(couponId).orElseThrow();

			// 시간 검증
			if (LocalDateTime.now().isBefore(coupon.getOpenAt())) {
				log.warn("Coupon issue failed: Not yet open. couponId={}", couponId);
				return false;
			}
			if (LocalDateTime.now().isAfter(coupon.getCloseAt())) {
				log.warn("Coupon issue failed: Already closed. couponId={}", couponId);
				return false;
			}

			// 실제 DB 저장 및 카운트 증가
			userCouponRepository.save(new UserCoupon(user, coupon, coupon.getExpirationDate()));
			coupon.decreaseQuantity();

			return true;
		} catch (Exception e) {
			log.error("Failed to issue coupon in consumer: {}", e.getMessage());
			return false;
		}
	}
}
