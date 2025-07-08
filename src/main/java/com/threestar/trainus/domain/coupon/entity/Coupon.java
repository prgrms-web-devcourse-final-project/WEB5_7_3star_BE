package com.threestar.trainus.domain.coupon.entity;

import com.threestar.trainus.global.entity.BaseDateEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private List<UserCoupon> userCoupons=new ArrayList<>();

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
