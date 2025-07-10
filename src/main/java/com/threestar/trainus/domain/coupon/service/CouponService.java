package com.threestar.trainus.domain.coupon.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.coupon.dto.CreateUserCouponResponseDto;
import com.threestar.trainus.domain.coupon.entity.Coupon;
import com.threestar.trainus.domain.coupon.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.entity.UserCoupon;
import com.threestar.trainus.domain.coupon.mapper.UserCouponMapper;
import com.threestar.trainus.domain.coupon.repository.CouponRepository;
import com.threestar.trainus.domain.coupon.repository.UserCouponRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponService {

	private final CouponRepository couponRepository;
	private final UserCouponRepository userCouponRepository;
	private final UserRepository userRepository;

	@Transactional
	public CreateUserCouponResponseDto createUserCoupon(Long userId, Long couponId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		Coupon coupon = couponRepository.findById(couponId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

		//쿠폰 발급 종료시각이 지났으면 예외처리
		if (LocalDateTime.now().isAfter(coupon.getCloseAt())) {
			throw new BusinessException(ErrorCode.COUPON_EXPIRED);
		}
		//선착순 쿠폰 발급 시 오픈시각 전이라면 예외처리
		if (coupon.getCategory() == CouponCategory.OPEN_RUN && LocalDateTime.now().isBefore(coupon.getOpenAt())) {
			throw new BusinessException(ErrorCode.COUPON_NOT_YET_OPEN);
		}
		//중복 발급 방지
		boolean alreadyIssued = userCouponRepository.existsByUserIdAndCouponId(userId, couponId);
		if (alreadyIssued) {
			throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
		}
		LocalDateTime expirationDate = coupon.getExpirationDate();

		UserCoupon userCoupon = new UserCoupon(user, coupon, expirationDate);
		userCouponRepository.save(userCoupon);

		return UserCouponMapper.toCreateUserCouponResponseDto(userCoupon);
	}
}
