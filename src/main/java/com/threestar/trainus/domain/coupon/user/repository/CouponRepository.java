package com.threestar.trainus.domain.coupon.user.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threestar.trainus.domain.coupon.user.dto.CouponResponseDto;
import com.threestar.trainus.domain.coupon.user.entity.Coupon;
import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;

import jakarta.persistence.LockModeType;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

	@Query("""
		SELECT new com.threestar.trainus.domain.coupon.user.dto.CouponResponseDto(
			c.id, c.name, c.discountPrice, c.minOrderPrice, c.expirationDate,
			CASE WHEN uc.id IS NOT NULL THEN com.threestar.trainus.domain.coupon.user.entity.OwnedStatus.OWNED
			ELSE com.threestar.trainus.domain.coupon.user.entity.OwnedStatus.NOT_OWNED END,
			c.quantity, c.category, c.openAt
		)
		FROM Coupon c
		LEFT JOIN UserCoupon uc ON c.id = uc.coupon.id AND uc.user.id = :userId
		WHERE c.closeAt > :now
		AND c.deletedAt IS NULL
		ORDER BY c.openAt ASC
		""")
	List<CouponResponseDto> findAvailableCouponsWithOwnership(@Param("userId") Long userId,
		@Param("now") LocalDateTime now);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT c from Coupon c where c.id = :couponId")
	Optional<Coupon> findByIdWithPessimisticLock(Long couponId);

	//관리자 쿠폰 목록 조회 -> 필터링
	@Query("""
		SELECT c FROM Coupon c
		WHERE (:status IS NULL OR c.status = :status)
		AND (:category IS NULL OR c.category = :category)
		AND c.deletedAt IS NULL
		ORDER BY c.createdAt DESC
		""")
	Page<Coupon> findCouponsWithFilters(
		@Param("status") CouponStatus status,
		@Param("category") CouponCategory category,
		Pageable pageable
	);

	//활성화할 쿠폰을 찾는 메서드
	// 현재는 비활성화 상태이지만, 오픈시간에 도달했으면서 마감기한이 안지난 쿠폰들
	@Query("""
		SELECT c FROM Coupon c
		WHERE c.status = com.threestar.trainus.domain.coupon.user.entity.CouponStatus.INACTIVE
		AND c.openAt <= :now
		AND c.closeAt > :now
		AND c.deletedAt IS NULL
		""")
	List<Coupon> findInactiveCouponsToActivate(@Param("now") LocalDateTime now);

	//비활성화 할 쿠폰을 찾는 메서드
	//현재 활성화 상태이지만, 마감시간이 지난 쿠폰들을 검사
	@Query("""
		SELECT c FROM Coupon c
		WHERE c.status = com.threestar.trainus.domain.coupon.user.entity.CouponStatus.ACTIVE
		AND c.closeAt <= :now
		AND c.deletedAt IS NULL
		""")
	List<Coupon> findActiveCouponsToDeactivate(@Param("now") LocalDateTime now);
}
