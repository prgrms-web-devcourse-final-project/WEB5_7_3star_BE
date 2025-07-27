package com.threestar.trainus.domain.coupon.user.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;
import com.threestar.trainus.domain.coupon.user.entity.UserCoupon;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {
	boolean existsByUserIdAndCouponId(Long userId, Long couponId);

	Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);

	@Query("SELECT uc FROM UserCoupon uc JOIN FETCH uc.coupon WHERE uc.user.id = :userId")
	List<UserCoupon> findAllByUserIdWithCoupon(@Param("userId") Long userId);

	@Query("SELECT uc FROM UserCoupon uc JOIN FETCH uc.coupon WHERE uc.user.id = :userId AND uc.status = :status")
	List<UserCoupon> findAllByUserIdAndStatusWithCoupon(@Param("userId") Long userId,
		@Param("status") CouponStatus status);

	// 특정 쿠폰의 발급 수 조회
	Long countByCouponId(Long couponId);

	// 특정 사용자가 특정 쿠폰을 발급받은 수 조회
	Long countByUserIdAndCouponId(Long userId, Long couponId);

	//만료시킬 유저쿠폰을 찾는 메서드
	//현재 활성화 상태이지만, 사용 유효기간이 지난 유저쿠폰들임!
	@Query("""
		SELECT uc FROM UserCoupon uc
		WHERE uc.status = com.threestar.trainus.domain.coupon.user.entity.CouponStatus.ACTIVE
		AND uc.expirationDate <= :now
		""")
	List<UserCoupon> findActiveUserCouponsToExpire(@Param("now") LocalDateTime now);

}
