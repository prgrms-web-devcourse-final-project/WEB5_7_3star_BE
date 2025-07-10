package com.threestar.trainus.domain.profile.mapper;

import com.threestar.trainus.domain.profile.dto.ProfileResponseDto;
import com.threestar.trainus.domain.profile.dto.ProfileUpdateRequestDto;
import com.threestar.trainus.domain.profile.entity.Profile;
import com.threestar.trainus.domain.user.entity.User;

public class ProfileMapper {

	private ProfileMapper() {

	}

	public static ProfileResponseDto toResponseDto(Profile profile, User user) {
		return new ProfileResponseDto(
			user.getId(),
			user.getNickname(),
			profile.getProfileImage(),
			profile.getIntro()
		);
	}

	public static Profile toUpdateEntity(Profile profile, ProfileUpdateRequestDto requestDto) {
		return Profile.builder()
			.id(profile.getId())
			.user(profile.getUser())
			.profileImage(requestDto.profileImage())
			.intro(requestDto.intro())
			.build();
	}

	public static Profile toDefaultEntity(User user) {
		return Profile.builder()
			.user(user)
			.profileImage(null)
			.intro(null)
			.build();
	}
}
