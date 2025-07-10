package com.threestar.trainus.domain.profile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.metadata.dto.ProfileMetadataResponseDto;
import com.threestar.trainus.domain.metadata.service.ProfileMetadataService;
import com.threestar.trainus.domain.profile.dto.ProfileDetailResponseDto;
import com.threestar.trainus.domain.profile.dto.ProfileResponseDto;
import com.threestar.trainus.domain.profile.mapper.ProfileDetailMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileFacadeService {

	private final ProfileService profileService;
	private final ProfileMetadataService metadataService;

	//프로필 dto랑 메타데이터 dto랑 합쳐서 응답을 내려주는 역할.
	@Transactional
	public ProfileDetailResponseDto getProfileDetail(Long userId) {

		ProfileResponseDto profile = profileService.getProfile(userId);

		ProfileMetadataResponseDto metadata = metadataService.getMetadata(userId);

		return ProfileDetailMapper.combineToDetailDto(profile, metadata);
	}
}
