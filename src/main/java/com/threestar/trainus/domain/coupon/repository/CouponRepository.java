package com.threestar.trainus.domain.coupon.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threestar.trainus.domain.coupon.entity.Coupon;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
}
