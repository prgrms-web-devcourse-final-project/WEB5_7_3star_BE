package com.threestar.trainus.domain.payment.entity;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.coupon.user.entity.UserCoupon;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.global.entity.BaseDateEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "payments")
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseDateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lesson_id", nullable = false)
	private Lesson lesson;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_coupon_id")
	private UserCoupon userCoupon;

	@Column(nullable = false)
	private String orderId;
	private Integer originPrice;
	private Integer payPrice;
	private LocalDateTime payDate;
	private LocalDateTime refundDate;
	private LocalDateTime cancelledAt;

	@Enumerated(EnumType.STRING)
	private PaymentStatus status;

	@Enumerated(EnumType.STRING)
	private PaymentMethod paymentMethod;

}
