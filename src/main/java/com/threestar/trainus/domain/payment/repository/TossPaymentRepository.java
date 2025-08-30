package com.threestar.trainus.domain.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threestar.trainus.domain.payment.entity.TossPayment;

public interface TossPaymentRepository extends JpaRepository<TossPayment, Long> {

	Optional<TossPayment> findByOrderId(String orderId);

	@Query("""
		SELECT t FROM TossPayment t
		WHERE t.orderId IN :orderIds
	""")
	List<TossPayment> findAllByOrderIds(@Param("orderIds") List<String> orderIds);
}
