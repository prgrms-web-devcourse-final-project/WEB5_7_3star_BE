package com.threestar.trainus.domain.profile.mapper;

import com.threestar.trainus.domain.metadata.dto.ProfileMetadataResponseDto;
import com.threestar.trainus.domain.profile.dto.ProfileDetailResponseDto;
import com.threestar.trainus.domain.profile.dto.ProfileResponseDto;


public class ProfileDetailMapper {

	private ProfileDetailMapper() {
	}

	public static ProfileDetailResponseDto combineToDetailDto(
		ProfileResponseDto profile,
		ProfileMetadataResponseDto metadata
	) {
		return new ProfileDetailResponseDto(
			profile.userId(),
			profile.nickname(),
			profile.profileImage(),
			profile.intro(),
			metadata.reviewCount(),
			metadata.rating()
		);
	}
}
