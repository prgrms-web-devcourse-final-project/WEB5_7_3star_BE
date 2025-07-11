package com.threestar.trainus.domain.coupon.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threestar.trainus.domain.coupon.entity.CouponStatus;
import com.threestar.trainus.domain.coupon.entity.UserCoupon;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
	boolean existsByUserIdAndCouponId(Long userId, Long couponId);

	@Query("SELECT uc FROM UserCoupon uc JOIN FETCH uc.coupon WHERE uc.user.id = :userId")
	List<UserCoupon> findAllByUserIdWithCoupon(@Param("userId") Long userId);

	@Query("SELECT uc FROM UserCoupon uc JOIN FETCH uc.coupon WHERE uc.user.id = :userId AND uc.status = :status")
	List<UserCoupon> findAllByUserIdAndStatusWithCoupon(@Param("userId") Long userId,
		@Param("status") CouponStatus status);
}
