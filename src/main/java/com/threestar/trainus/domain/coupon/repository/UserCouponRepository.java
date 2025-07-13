package com.threestar.trainus.domain.coupon.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threestar.trainus.domain.coupon.entity.CouponStatus;
import com.threestar.trainus.domain.coupon.entity.UserCoupon;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
	boolean existsByUserIdAndCouponId(Long userId, Long couponId);

	List<UserCoupon> findAllByUserId(Long userId);

	List<UserCoupon> findAllByUserIdAndStatus(Long userId, CouponStatus status);

	Long countByUserIdAndCouponId(Long id, Long id1);

	Long countByCouponId(Long id);
}
