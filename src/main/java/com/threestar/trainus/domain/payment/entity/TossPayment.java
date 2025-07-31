package com.threestar.trainus.domain.payment.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TossPayment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String paymentKey;

	//토스 내부에서 관리하는 별도의 orderId
	@Column(nullable = false)
	private String orderId;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "payment_id", nullable = false)
	private Payment payment;

	private Integer amount;
	private String orderName;

	@Enumerated(value = EnumType.STRING)
	@Column(nullable = false)
	private PaymentStatus paymentStatus;

	@Enumerated(value = EnumType.STRING)
	@Column(nullable = false)
	private PaymentMethod paymentMethod;

	@Column(nullable = false)
	private LocalDateTime requestedAt;

	private LocalDateTime approvedAt;
	private LocalDateTime canceledAt;

	private String cancelReason;

	public void changeStatus(LocalDateTime canceledAt, PaymentStatus paymentStatus, String cancelReason) {
		this.canceledAt = canceledAt;
		this.paymentStatus = paymentStatus;
		this.cancelReason = cancelReason;
	}
}
