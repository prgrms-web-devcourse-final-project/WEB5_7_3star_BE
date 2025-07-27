package com.threestar.trainus.domain.metadata.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.threestar.trainus.domain.metadata.entity.ProfileMetadata;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;

class ProfileMetadataTest {

	@Test
	@DisplayName("리뷰 개수를 증가시킬 수 있다")
	void increaseReviewCount_shouldIncreaseCount() {
		// given
		User user = User.builder()
			.email("test@example.com")
			.password("password")
			.nickname("testUser")
			.role(UserRole.USER)
			.build();

		ProfileMetadata metadata = ProfileMetadata.builder()
			.user(user)
			.reviewCount(5)
			.rating(4.0)
			.build();

		// when
		metadata.increaseReviewCount();

		// then
		assertThat(metadata.getReviewCount()).isEqualTo(6);
	}

	@Test
	@DisplayName("새로운 평점을 반영하여 평균 평점을 계산한다")
	void updateRating_shouldCalculateAverageRating() {
		// given
		User user = User.builder()
			.email("test@example.com")
			.password("password")
			.nickname("testUser")
			.role(UserRole.USER)
			.build();

		ProfileMetadata metadata = ProfileMetadata.builder()
			.user(user)
			.reviewCount(3)
			.rating(4.0)
			.build();

		double newRating = 5.0;

		// when
		double updatedRating = metadata.updateRating(newRating);

		// then
		// 기존: (4.0 * 2) + 5.0 = 13.0 / 3 = 4.33...
		assertThat(updatedRating).isCloseTo(4.33, within(0.01));
	}

	@Test
	@DisplayName("첫 번째 리뷰인 경우 평점이 그대로 반영된다")
	void updateRating_firstReview_shouldReturnNewRating() {
		// given
		User user = User.builder()
			.email("test@example.com")
			.password("password")
			.nickname("testUser")
			.role(UserRole.USER)
			.build();

		ProfileMetadata metadata = ProfileMetadata.builder()
			.user(user)
			.reviewCount(1)
			.rating(0.0)
			.build();

		double newRating = 5.0;

		// when
		double updatedRating = metadata.updateRating(newRating);

		// then
		assertThat(updatedRating).isEqualTo(5.0);
	}
}