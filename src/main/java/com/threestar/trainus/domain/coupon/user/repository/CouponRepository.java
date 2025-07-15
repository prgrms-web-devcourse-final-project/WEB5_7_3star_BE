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
			CASE WHEN uc.id IS NOT NULL THEN "OWNED" ELSE "NOT_OWNED" END,
			c.quantity, c.category, c.openAt
		)
		FROM Coupon c
		LEFT JOIN UserCoupon uc ON c.id = uc.coupon.id AND uc.user.id = :userId
		WHERE c.closeAt > :now
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
		ORDER BY c.createdAt DESC
		""")
	Page<Coupon> findCouponsWithFilters(
		@Param("status") CouponStatus status,
		@Param("category") CouponCategory category,
		Pageable pageable
	);
}
