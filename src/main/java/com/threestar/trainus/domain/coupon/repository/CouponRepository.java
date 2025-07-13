package com.threestar.trainus.domain.coupon.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.threestar.trainus.domain.coupon.entity.Coupon;

import jakarta.persistence.LockModeType;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
	List<Coupon> findAllByCloseAtAfter(LocalDateTime now);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT c from Coupon c where c.id = :couponId")
	Optional<Coupon> findByIdWithPessimisticLock(Long couponId);
}
