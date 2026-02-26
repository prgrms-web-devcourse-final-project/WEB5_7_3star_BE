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

@Service
@RequiredArgsConstructor
public class CouponIssueService {
	private final CouponRepository couponRepository;
	private final UserCouponRepository userCouponRepository;
	private final UserService userService;

	@Transactional
	public boolean issue(Long couponId, Long userId) {
		//1.멱등처리(이미 발급된 경우)
		if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
			return false;
		}
		User user = userService.getUserById(userId);
		Coupon coupon = couponRepository.findById(couponId).orElseThrow();

		//2. 시간 검증
		if (LocalDateTime.now().isBefore(coupon.getOpenAt())) {
			return false;
		}
		if (LocalDateTime.now().isAfter(coupon.getCloseAt())) {
			return false;
		}

		userCouponRepository.save(new UserCoupon(user, coupon, coupon.getExpirationDate()));
		coupon.decreaseQuantity();

		return true;
	}
}
