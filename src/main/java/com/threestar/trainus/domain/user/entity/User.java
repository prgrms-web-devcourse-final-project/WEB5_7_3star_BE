package com.threestar.trainus.domain.user.entity;

import com.threestar.trainus.domain.coupon.entity.UserCoupon;
import com.threestar.trainus.domain.profile.entity.Profile;
import com.threestar.trainus.global.entity.BaseDateEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class User extends BaseDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30, nullable = false, unique = true)
    private String email;

    @Column(length = 100, nullable = false)
    private String password;

    @Column(length = 20, nullable = false, unique = true)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    //createdAt
    //updatedAt

    private LocalDateTime deletedAt;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Profile profile;

    @OneToMany(mappedBy = "user")
    private List<UserCoupon> userCoupons=new ArrayList<>();

}
