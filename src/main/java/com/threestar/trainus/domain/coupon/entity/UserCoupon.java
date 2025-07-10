package com.threestar.trainus.domain.coupon.entity;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.global.entity.BaseDateEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_coupons")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon extends BaseDateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "coupon_id", nullable = false)
	private Coupon coupon;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	private LocalDateTime useDate;

	@Enumerated(EnumType.STRING)
	private CouponStatus status;

	private LocalDateTime expirationDate;

	public UserCoupon(User user, Coupon coupon, LocalDateTime expirationDate) {
		this.user = user;
		this.coupon = coupon;
		this.expirationDate = expirationDate;
		this.status = CouponStatus.ACTIVE;
	}
}
