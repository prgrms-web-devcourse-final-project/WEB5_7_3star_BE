package com.threestar.trainus.domain.coupon.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threestar.trainus.domain.coupon.entity.UserCoupon;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
}
