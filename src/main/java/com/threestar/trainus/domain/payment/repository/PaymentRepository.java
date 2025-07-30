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

	Optional<Payment> findByUserAndLessonAndUserCouponAndStatus(User user, Lesson lesson, UserCoupon coupon,
		PaymentStatus status);

	Optional<Payment> findByUserAndLessonAndUserCouponIsNullAndStatus(User user, Lesson lesson, PaymentStatus status);

	@Query(value = """
		SELECT p.id FROM payments p
		WHERE p.user_id = :userId
		AND p.status = :status
		ORDER BY p.pay_date DESC 
		LIMIT :limit OFFSET :offset
		""", nativeQuery = true)
	List<Long> findPaymentIdsByUserAndStatus(
		@Param("userId") Long userId,
		@Param("status") String status,
		@Param("offset") int offset,
		@Param("limit") int limit
	);

	@Query("""
			SELECT p FROM Payment p
			LEFT JOIN FETCH p.lesson
			LEFT JOIN FETCH p.userCoupon
			WHERE p.id IN :ids
		""")
	List<Payment> findAllWithAssociationsByIds(@Param("ids") List<Long> ids);

	@Query(value = """
			SELECT count(*) FROM (SELECT id FROM payments WHERE user_id = :userId AND status = :status LIMIT :limit) t
		""", nativeQuery = true
	)
	Integer count(
		@Param("userId") Long userId,
		@Param("status") String status,
		@Param("limit") int limit
	);

	boolean existsByLessonAndUserAndStatusIn(Lesson lesson, User user, List<PaymentStatus> statuses);
}
