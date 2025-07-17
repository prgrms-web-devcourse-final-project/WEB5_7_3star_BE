package com.threestar.trainus.domain.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threestar.trainus.domain.payment.entity.TossPayment;

public interface TossPaymentRepository extends JpaRepository<TossPayment, Long> {
	Optional<TossPayment> findByPaymentKey(String paymentKey);
}
