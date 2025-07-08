package com.threestar.trainus.domain.coupon.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.threestar.trainus.global.entity.BaseDateEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coupons")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseDateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToMany(mappedBy = "coupon")
	private List<UserCoupon> userCoupons = new ArrayList<>();

	@Column(length = 45, nullable = false)
	private String name;

	private LocalDateTime expirationDate;

	@Column(nullable = false)
	private String discountPrice;

	@Column(nullable = false)
	private Integer minOrderPrice;

	@Column(length = 10, nullable = false)
	@Enumerated(EnumType.STRING)
	private CouponStatus status;

	private Integer quantity;

	@Column(length = 20, nullable = false)
	@Enumerated(EnumType.STRING)
	private CouponCategory category;

	@Column(nullable = false)
	private LocalDateTime openAt;

	@Column(nullable = false)
	private LocalDateTime closeAt;

}
