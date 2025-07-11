package com.threestar.trainus.domain.coupon.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;


import com.threestar.trainus.domain.coupon.dto.CouponResponseDto;
import com.threestar.trainus.domain.coupon.entity.Coupon;

import jakarta.persistence.LockModeType;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

	@Query("""
		SELECT new com.threestar.trainus.domain.coupon.dto.CouponResponseDto(
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

}
