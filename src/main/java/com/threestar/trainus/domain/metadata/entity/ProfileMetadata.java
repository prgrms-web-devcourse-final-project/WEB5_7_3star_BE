package com.threestar.trainus.domain.metadata.entity;

import com.threestar.trainus.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "profile_metadata")
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileMetadata {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private Integer reviewCount;

	@Setter
	@Column(nullable = false)
	private Float rating;

	public void increaseReviewCount() {
		this.reviewCount++;
	}

	public Float updateRating(Float newRating) {
		Float totalRatingBefore = this.rating * (reviewCount - 1);
		totalRatingBefore += newRating;
		return totalRatingBefore / reviewCount;
	}
}
