package com.threestar.trainus.domain.metadata.mapper;

import com.threestar.trainus.domain.metadata.dto.ProfileMetadataResponseDto;
import com.threestar.trainus.domain.metadata.entity.ProfileMetadata;
import com.threestar.trainus.domain.metadata.repositroy.ProfileMetadataRepository;
import com.threestar.trainus.domain.user.entity.User;

public class ProfileMetadataMapper {

	private ProfileMetadataMapper() {
	}

	public static ProfileMetadataResponseDto toResponseDto(ProfileMetadata profileMetadata, User user) {
		return new ProfileMetadataResponseDto(
			user.getId(),
			profileMetadata.getReviewCount(),
			profileMetadata.getRating()
		);
	}

	public static ProfileMetadata toDefaultEntity(User user) {
		return ProfileMetadata.builder()
			.user(user)
			.reviewCount(0)
			.rating(0.0F)
			.build();
	}
}

