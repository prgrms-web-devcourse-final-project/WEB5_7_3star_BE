package com.threestar.trainus.domain.user.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.threestar.trainus.domain.coupon.entity.UserCoupon;
import com.threestar.trainus.domain.metadata.entity.ProfileMetadata;
import com.threestar.trainus.domain.profile.entity.Profile;
import com.threestar.trainus.global.entity.BaseDateEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

	private LocalDateTime deletedAt;

	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
	private Profile profile;

	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
	private ProfileMetadata profileMetadata;

	@OneToMany(mappedBy = "user")
	private List<UserCoupon> userCoupons = new ArrayList<>();

}
