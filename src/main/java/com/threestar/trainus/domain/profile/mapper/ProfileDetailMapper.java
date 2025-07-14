package com.threestar.trainus.domain.profile.mapper;

import com.threestar.trainus.domain.metadata.dto.ProfileMetadataResponseDto;
import com.threestar.trainus.domain.profile.dto.ProfileDetailResponseDto;
import com.threestar.trainus.domain.profile.dto.ProfileResponseDto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileDetailMapper {

	public static ProfileDetailResponseDto toDetailResponseDto(
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
