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
		select p.payment_id from payments p
		where p.user_id = :userId
		and p.status = :status
		order by p.pay_date desc
		limit :limit offset :offset
		""", nativeQuery = true)
	List<Long> findPaymentIdsByUserAndStatus(
		@Param("userId") Long userId,
		@Param("status") String status,
		@Param("offset") int offset,
		@Param("limit") int limit
	);

	@Query("""
			select p from Payment p
			left join fetch p.lesson
			left join fetch p.userCoupon
			where p.paymentId in :ids
		""")
	List<Payment> findAllWithAssociationsByIds(@Param("ids") List<Long> ids);

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
