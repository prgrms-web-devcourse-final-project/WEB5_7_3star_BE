package com.threestar.trainus.domain.coupon.admin.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.coupon.admin.dto.CouponCreateRequestDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponCreateResponseDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponDeleteResponseDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponDetailResponseDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponListResponseDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponUpdateRequestDto;
import com.threestar.trainus.domain.coupon.admin.dto.CouponUpdateResponseDto;
import com.threestar.trainus.domain.coupon.admin.mapper.AdminCouponMapper;
import com.threestar.trainus.domain.coupon.user.entity.Coupon;
import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;
import com.threestar.trainus.domain.coupon.user.repository.CouponRepository;
import com.threestar.trainus.domain.coupon.user.repository.UserCouponRepository;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminCouponService {

	private final CouponRepository couponRepository;
	private final UserCouponRepository userCouponRepository;
	private final UserService userService;

	//쿠폰 생성
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

	//쿠폰 조회
	@Transactional(readOnly = true)
	public CouponListResponseDto getCoupons(int page, int limit, CouponStatus status, CouponCategory category,
		Long userId) {
		// 관리자 권한 검증
		userService.validateAdminRole(userId);

		Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());

		// 조건에 따른 쿠폰 조회
		Page<Coupon> couponPage = couponRepository.findCouponsWithFilters(status, category, pageable);

		//응답 DTO 변환
		return AdminCouponMapper.toCouponListResponseDto(couponPage);
	}

	//쿠폰 상세 조횧
	@Transactional(readOnly = true)
	public CouponDetailResponseDto getCouponDetail(Long couponId, Long userId) {
		// 관리자 권한 검증
		userService.validateAdminRole(userId);

		// 쿠폰 조회
		Coupon coupon = findCouponById(couponId);

		// 발급된 쿠폰 수 조회
		Long issuedCountLong = userCouponRepository.countByCouponId(couponId);
		Integer issuedCount = issuedCountLong.intValue();

		// 응답 DTO 변환
		return AdminCouponMapper.toCouponDetailResponseDto(coupon, issuedCount);
	}

	//쿠폰 수정
	@Transactional
	public CouponUpdateResponseDto updateCoupon(Long couponId, CouponUpdateRequestDto request, Long userId) {
		// 관리자 권한 검증
		userService.validateAdminRole(userId);

		// 쿠폰 조회
		Coupon coupon = findCouponById(couponId);

		// 발급된 쿠폰 수 조회
		Long issuedCount = userCouponRepository.countByCouponId(couponId);

		// 수정 검증 로직
		validateCouponUpdate(coupon, request, issuedCount);

		// 수정된 쿠폰 업데이트
		updateCouponFields(coupon, request);

		Coupon updatedCoupon = couponRepository.save(coupon);

		// 응답 DTO 변환
		return AdminCouponMapper.toCouponUpdateResponseDto(updatedCoupon);
	}

	//쿠폰 삭제
	@Transactional
	public CouponDeleteResponseDto deleteCoupon(Long couponId, Long userId) {
		// 관리자 권한 검증
		userService.validateAdminRole(userId);

		// 쿠폰 조회
		Coupon coupon = findCouponById(couponId);

		// 이미 삭제된 쿠폰인지 확인
		if (coupon.isDeleted()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
		}

		// 발급된 쿠폰 수 조회
		Long issuedCount = userCouponRepository.countByCouponId(couponId);

		// 삭제 가능 여부 검증 (발급된 쿠폰이 있으면 예외 발생)
		validateCouponDeletion(coupon, issuedCount);

		// 쿠폰 삭제 처리(아무도 발급받지 않은 쿠폰만)
		coupon.markAsDeleted();
		Coupon deletedCoupon = couponRepository.save(coupon);

		// 응답 DTO 변환
		return AdminCouponMapper.toCouponDeleteResponseDto(deletedCoupon);
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

		// 상태와 오픈 시간 논리적으로 맞는지 검증
		validateStatusConsistency(request.status(), request.couponOpenAt());

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

	//status랑 오픈시각이랑 검증 메서드 -> open시간이 미래인데 active일경우
	private void validateStatusConsistency(CouponStatus status, LocalDateTime openAt) {
		LocalDateTime now = LocalDateTime.now();

		// ACTIVE 상태인데 오픈 시간이 미래인 경우
		if (status == CouponStatus.ACTIVE && openAt.isAfter(now)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
		}

		// INACTIVE 상태인데 오픈 시간이 과거인 경우
		if (status == CouponStatus.INACTIVE && openAt.isBefore(now)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
		}
	}

	private void validateCouponUpdate(Coupon coupon, CouponUpdateRequestDto request, Long issuedCount) {
		LocalDateTime now = LocalDateTime.now();

		// 선착순 쿠폰인데 수량이 없는 경우
		if (request.category() != null) {
			if (request.category() == CouponCategory.OPEN_RUN && request.quantity() == null) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
			}
		}

		// 수량 수정 검증 -> 선착순 쿠폰의 경우
		if (request.quantity() != null) {
			// 이미 발급된 수량보다 적게 설정 불가
			if (request.quantity() < issuedCount.intValue()) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
			}
		}

		// 카테고리 변경 검증 -> 이미 발급한 쿠폰이 있다면 변경 못하게 막기
		if (request.category() != null && issuedCount > 0) {
			// 이미 발급된 쿠폰이 있으면 카테고리 변경 제한
			if (!coupon.getCategory().equals(request.category())) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
			}
		}

		// 선착순 → 일반 변경은 아무도 발급받지 않은 경우만 가능
		if (request.category() == CouponCategory.NORMAL && coupon.getCategory() == CouponCategory.OPEN_RUN
			&& issuedCount > 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
		}

		// 이미 오픈된 쿠폰의 오픈시각은 수정 불가
		if (request.couponOpenAt() != null) {
			if (coupon.getOpenAt().isBefore(now)) {
				// 이미 오픈된 쿠폰의 오픈 시각은 수정 불가
				throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
			}

			// 새로운 오픈 시각이 마감 시각보다 늦으면 안됨
			LocalDateTime newCloseAt =
				request.couponDeadlineAt() != null ? request.couponDeadlineAt() : coupon.getCloseAt();
			if (request.couponOpenAt().isAfter(newCloseAt)) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
			}
		}

		// 이미 마감된 쿠폰의 마감시각은 수정 불가 -> 진행중인경우 연장만 가능
		if (request.couponDeadlineAt() != null) {
			if (coupon.getCloseAt().isBefore(now)) {
				// 이미 마감된 쿠폰의 마감 시각은 수정 불가
				throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
			}

			if (coupon.getOpenAt().isBefore(now) && coupon.getCloseAt().isAfter(now)) {
				// 진행 중인 쿠폰은 연장만 가능 -> 단축은 못함
				if (request.couponDeadlineAt().isBefore(coupon.getCloseAt())) {
					throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
				}
			}

			// 새로운 마감 시각이 오픈 시각보다 이르면 안됨
			LocalDateTime newOpenAt = request.couponOpenAt() != null ? request.couponOpenAt() : coupon.getOpenAt();
			if (request.couponDeadlineAt().isBefore(newOpenAt)) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA);
			}
		}
	}

	private void updateCouponFields(Coupon coupon, CouponUpdateRequestDto request) {
		if (request.couponName() != null) {
			coupon.updateName(request.couponName());
		}

		if (request.status() != null) {
			// INACTIVE → ACTIVE로 변경 시, 오픈시각을 현재 시간으로 업데이트
			if (request.status() == CouponStatus.ACTIVE && coupon.getStatus() == CouponStatus.INACTIVE) {
				LocalDateTime now = LocalDateTime.now();
				if (coupon.getOpenAt().isAfter(now)) {
					coupon.updateOpenAt(now);
				}
			}

			coupon.updateStatus(request.status());
		}

		// 카테고리 변경 시 수량도 함께 처리
		if (request.category() != null) {
			coupon.updateCategory(request.category());

			if (request.category() == CouponCategory.NORMAL) {
				// 일반 쿠폰으로 변경 시 수량을 null로 설정
				coupon.updateQuantity(null);
			} else if (request.category() == CouponCategory.OPEN_RUN) {
				// 선착순 쿠폰으로 변경 시 수량 설정
				if (request.quantity() != null) {
					coupon.updateQuantity(request.quantity());
				}
			}
		}

		// 수량만 변경하는 경우 -> 선착순 쿠폰일 경우만
		if (request.quantity() != null && request.category() == null) {
			// 현재 선착순 쿠폰인 경우에만 수량 변경 허용
			if (coupon.getCategory() == CouponCategory.OPEN_RUN) {
				coupon.updateQuantity(request.quantity());
			}
		}

		if (request.couponOpenAt() != null) {
			coupon.updateOpenAt(request.couponOpenAt());
		}

		if (request.couponDeadlineAt() != null) {
			coupon.updateCloseAt(request.couponDeadlineAt());
		}
	}

	private void validateCouponDeletion(Coupon coupon, Long issuedCount) {
		// 발급된 쿠폰이 하나라도 있으면 삭제 불가
		if (issuedCount > 0) {
			throw new BusinessException(ErrorCode.COUPON_CANNOT_DELETE_ISSUED);
		}
	}

	//쿠폰id로 쿠폰을 조회하는 공통 메서드
	public Coupon findCouponById(Long couponId) {
		return couponRepository.findById(couponId)
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST_DATA));
	}
}
