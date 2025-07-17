package com.threestar.trainus.domain.profile.mapper;

import com.threestar.trainus.domain.profile.dto.ImageUpdateResponseDto;
import com.threestar.trainus.domain.profile.dto.IntroUpdateResponseDto;
import com.threestar.trainus.domain.profile.dto.ProfileResponseDto;
import com.threestar.trainus.domain.profile.entity.Profile;
import com.threestar.trainus.domain.user.entity.User;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileMapper {

	public static ProfileResponseDto toResponseDto(Profile profile, User user) {
		return new ProfileResponseDto(
			user.getId(),
			user.getNickname(),
			profile.getProfileImage(),
			profile.getIntro()
		);
	}

	public static ImageUpdateResponseDto toImageResponseDto(Profile profile, User user) {
		return new ImageUpdateResponseDto(
			user.getId(),
			user.getNickname(),
			profile.getProfileImage()
		);
	}

	public static IntroUpdateResponseDto toIntroResponseDto(Profile profile, User user) {
		return new IntroUpdateResponseDto(
			user.getId(),
			user.getNickname(),
			profile.getIntro()
		);
	}

	public static Profile toDefaultEntity(User user) {
		return Profile.builder()
			.user(user)
			.profileImage(null)
			.intro(null)
			.build();
	}
}
