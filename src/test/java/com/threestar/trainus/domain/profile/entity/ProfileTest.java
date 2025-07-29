package com.threestar.trainus.domain.profile.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.threestar.trainus.domain.profile.entity.Profile;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;

class ProfileTest {

	@Test
	@DisplayName("프로필 이미지를 업데이트할 수 있다")
	void updateProfileImage_shouldUpdateImage() {
		// given
		User user = User.builder()
			.email("test@example.com")
			.password("password")
			.nickname("testUser")
			.role(UserRole.USER)
			.build();

		Profile profile = Profile.builder()
			.user(user)
			.profileImage("oldImage.jpg")
			.intro("old intro")
			.build();

		String newImage = "newImage.jpg";

		// when
		profile.updateProfileImage(newImage);

		// then
		assertThat(profile.getProfileImage()).isEqualTo(newImage);
	}

	@Test
	@DisplayName("프로필 소개를 업데이트할 수 있다")
	void updateProfileIntro_shouldUpdateIntro() {
		// given
		User user = User.builder()
			.email("test@example.com")
			.password("password")
			.nickname("testUser")
			.role(UserRole.USER)
			.build();

		Profile profile = Profile.builder()
			.user(user)
			.profileImage("image.jpg")
			.intro("old intro")
			.build();

		String newIntro = "new introduction";

		// when
		profile.updateProfileIntro(newIntro);

		// then
		assertThat(profile.getIntro()).isEqualTo(newIntro);
	}
}