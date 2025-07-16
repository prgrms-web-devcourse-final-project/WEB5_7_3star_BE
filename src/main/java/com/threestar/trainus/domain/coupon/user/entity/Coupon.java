package com.threestar.trainus.domain.coupon.user.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.threestar.trainus.global.entity.BaseDateEntity;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

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

	private LocalDateTime deletedAt;

	public void decreaseQuantity() {
		if (this.quantity <= 0) {
			throw new BusinessException(ErrorCode.COUPON_BE_EXHAUSTED);
		}
		this.quantity--;
	}

	//쿠폰 수정 관련 메서드 추가
	public void updateName(String name) {
		this.name = name;
	}

	public void updateStatus(CouponStatus status) {
		this.status = status;
	}

	public void updateQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public void updateCategory(CouponCategory category) {
		this.category = category;
	}

	public void updateOpenAt(LocalDateTime openAt) {
		this.openAt = openAt;
	}

	public void updateCloseAt(LocalDateTime closeAt) {
		this.closeAt = closeAt;
	}

	//삭제관련 메서드 추가
	public void markAsDeleted() {
		this.deletedAt = LocalDateTime.now();
	}

	//쿠폰이 삭제된 상태인지 확인
	public boolean isDeleted() {
		return this.deletedAt != null;
	}

	public LocalDateTime getDeletedAt() {
		return this.deletedAt;
	}

}
