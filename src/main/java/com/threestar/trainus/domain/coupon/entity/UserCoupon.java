package com.threestar.trainus.domain.coupon.entity;

import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.global.entity.BaseDateEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="user_coupons")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon extends BaseDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="coupon_id",nullable = false)
    private Coupon coupon;

    @ManyToOne
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    private LocalDateTime useDate;

    @Enumerated(EnumType.STRING)
    private CouponStatus status;

    private LocalDateTime expirationDate;


}
