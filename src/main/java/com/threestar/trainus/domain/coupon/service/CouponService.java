package com.threestar.trainus.domain.coupon.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.coupon.dto.CouponPageResponseDto;
import com.threestar.trainus.domain.coupon.dto.CouponResponseDto;
import com.threestar.trainus.domain.coupon.dto.CreateUserCouponResponseDto;
import com.threestar.trainus.domain.coupon.dto.UserCouponPageResponseDto;
import com.threestar.trainus.domain.coupon.dto.UserCouponResponseDto;
import com.threestar.trainus.domain.coupon.entity.Coupon;
import com.threestar.trainus.domain.coupon.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.entity.CouponStatus;
import com.threestar.trainus.domain.coupon.entity.UserCoupon;
import com.threestar.trainus.domain.coupon.mapper.CouponMapper;
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
		Coupon coupon = couponRepository.findByIdWithPessimisticLock(couponId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

		//쿠폰 발급 종료시각이 지났으면 예외처리
		if (LocalDateTime.now().isAfter(coupon.getCloseAt())) {
			throw new BusinessException(ErrorCode.COUPON_EXPIRED);
		}
		//중복 발급 방지
		boolean alreadyIssued = userCouponRepository.existsByUserIdAndCouponId(userId, couponId);
		if (alreadyIssued) {
			throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
		}
		if (coupon.getCategory() == CouponCategory.OPEN_RUN) {
			//선착순 쿠폰 발급 시 오픈시각 전이라면 예외처리
			if (LocalDateTime.now().isBefore(coupon.getOpenAt())) {
				throw new BusinessException(ErrorCode.COUPON_NOT_YET_OPEN);
			}
			//선착순 쿠폰 발급 시 수량이 소진되면 예외처리
			if (coupon.getQuantity() <= 0) {
				throw new BusinessException(ErrorCode.COUPON_BE_EXHAUSTED);
			}
			coupon.decreaseQuantity();
		}

		LocalDateTime expirationDate = coupon.getExpirationDate();

		UserCoupon userCoupon = new UserCoupon(user, coupon, expirationDate);
		userCouponRepository.save(userCoupon);

		return UserCouponMapper.toCreateUserCouponResponseDto(userCoupon);
	}

	@Transactional(readOnly = true)
	public UserCouponPageResponseDto getUserCoupons(Long userId, CouponStatus status) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		List<UserCoupon> userCoupons;

		if (status == null) {
			userCoupons = userCouponRepository.findAllByUserId(userId);
		} else {
			userCoupons = userCouponRepository.findAllByUserIdAndStatus(userId, status);
		}
		List<UserCouponResponseDto> couponDtos = UserCouponMapper.toDtoList(userCoupons);
		return new UserCouponPageResponseDto(couponDtos);
	}

	@Transactional(readOnly = true)
	public CouponPageResponseDto getCoupons(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		List<Coupon> coupons = couponRepository.findAllByCloseAtAfter(LocalDateTime.now());

		//유저가 가진 쿠폰 id목록
		List<Long> ownedCouponIds = userCouponRepository.findAllByUserId(userId).stream()
			.map(uc -> uc.getCoupon().getId())
			.toList();

		List<CouponResponseDto> dtoList =
			CouponMapper.toDtoList(coupons, ownedCouponIds);

		return CouponPageResponseDto.builder()
			.coupons(dtoList)
			.build();
	}
}
