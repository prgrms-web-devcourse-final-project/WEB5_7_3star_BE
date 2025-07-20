package com.threestar.trainus.domain.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threestar.trainus.domain.coupon.user.entity.UserCoupon;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.payment.entity.Payment;
import com.threestar.trainus.domain.payment.entity.PaymentStatus;
import com.threestar.trainus.domain.user.entity.User;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
	Optional<Payment> findByOrderId(String orderId);

	Optional<Payment> findByUserCouponAndStatus(UserCoupon coupon, PaymentStatus status);

	@Query(value = """
			select * from payments
			where user_id = :userId
			and status = :status
			order by pay_date desc
			limit :limit offset :offset
		""", nativeQuery = true
	)
	List<Payment> findAllByUserAndStatus(
		@Param("userId") Long userId,
		@Param("status") String status,
		@Param("offset") int offset,
		@Param("limit") int limit
	);

	@Query(value = """
			select count(*) from (select payment_id from payments where user_id = :userId and status = :status limit :limit) t
		""", nativeQuery = true
	)
	Integer count(
		@Param("userId") Long userId,
		@Param("status") String status,
		@Param("limit") int limit
	);

	boolean existsByLessonAndUserAndStatusIn(Lesson lesson, User user, List<PaymentStatus> statuses);
}
