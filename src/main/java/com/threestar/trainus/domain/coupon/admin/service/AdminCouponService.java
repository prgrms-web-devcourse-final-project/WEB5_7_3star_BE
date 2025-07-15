package com.threestar.trainus.domain.coupon.admin.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.coupon.admin.dto.CouponCreateRequestDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponCreateResponseDto;
import com.threestar.trainus.domain.coupon.admin.mapper.AdminCouponMapper;
import com.threestar.trainus.domain.coupon.user.entity.Coupon;
import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.user.repository.CouponRepository;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminCouponService {

	private final CouponRepository couponRepository;
	private final UserService userService;

	@Transactional
	public CouponCreateResponseDto createCoupon(CouponCreateRequestDto request, Long userId) {
		// 관리자 권한 검증
		userService.validateAdminRole(userId);

		// 쿠폰 생성 검증
		validateCouponRequest(request);

		Coupon coupon = AdminCouponMapper.toEntity(request);
		Coupon savedCoupon = couponRepository.save(coupon);

		// 응답 DTO 반환
		return AdminCouponMapper.toCreateResponseDto(savedCoupon);
	}

	private void validateCouponRequest(CouponCreateRequestDto request) {
		// 할인가격이 뭔지 검증(퍼센트인지 금액인지)
		validateDiscountPrice(request.discountPrice());

		// 오픈 시간이 마감 시간보다 늦으면 안됨
		if (request.couponOpenAt().isAfter(request.couponDeadlineAt())) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
		}

		// 오픈 시간이 현재 시간보다 과거면 안됨 -> 즉시 오픈은 허용함!
		if (request.couponOpenAt().isBefore(LocalDateTime.now().minusMinutes(1))) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
		}

		// 선착순 쿠폰 검증
		if (request.category() == CouponCategory.OPEN_RUN) {
			// 선착순 쿠폰은 수량이 필수
			if (request.quantity() == null || request.quantity() <= 0) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
			}
		}

		// 일반 쿠폰의 경우 수량은 null도 허용
		if (request.category() == CouponCategory.NORMAL) {
			// 수량이 설정된 경우에만 검증
			if (request.quantity() != null && request.quantity() <= 0) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
			}
		}

	}

	//할인 형식을 검증
	private void validateDiscountPrice(String discountPrice) {
		if (discountPrice == null || discountPrice.trim().isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
		}

		String cutPrice = discountPrice.trim();

		if (cutPrice.endsWith("%")) {
			// 퍼센트 할인 검증
			String percentStr = cutPrice.substring(0, cutPrice.length() - 1);
			try {
				int percent = Integer.parseInt(percentStr);
				if (percent <= 0 || percent > 100) {
					throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
				}
			} catch (NumberFormatException e) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
			}
		} else {
			// 금액 할인 검증
			try {
				int amount = Integer.parseInt(cutPrice);
				if (amount <= 0) {
					throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
				}
			} catch (NumberFormatException e) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
			}
		}
	}
}
