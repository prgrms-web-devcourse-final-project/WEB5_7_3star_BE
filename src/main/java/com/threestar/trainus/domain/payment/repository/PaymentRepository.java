package com.threestar.trainus.domain.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threestar.trainus.domain.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
	Optional<Payment> findByOrderId(String orderId);
}
