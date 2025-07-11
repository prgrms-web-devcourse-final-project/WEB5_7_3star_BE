package com.threestar.trainus.domain.profile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.metadata.dto.ProfileMetadataResponseDto;
import com.threestar.trainus.domain.metadata.service.ProfileMetadataService;
import com.threestar.trainus.domain.profile.dto.ProfileDetailResponseDto;
import com.threestar.trainus.domain.profile.dto.ProfileResponseDto;
import com.threestar.trainus.domain.profile.dto.ProfileUpdateRequestDto;
import com.threestar.trainus.domain.profile.mapper.ProfileDetailMapper;
import com.threestar.trainus.domain.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileFacadeService {

	private final ProfileService profileService;
	private final ProfileMetadataService metadataService;

	//프로필 상세 조회 (단순 프로필 + 메타데이터) 단순 프로필 조회는 제거.
	@Transactional(readOnly = true)
	public ProfileDetailResponseDto getProfileDetail(Long userId) {
		ProfileResponseDto profile = profileService.getProfile(userId);
		ProfileMetadataResponseDto metadata = metadataService.getMetadata(userId);

		return ProfileDetailMapper.toDetailResponseDto(profile, metadata);
	}

	//프로필 수정
	@Transactional
	public ProfileResponseDto updateProfile(Long userId, ProfileUpdateRequestDto requestDto) {
		return profileService.updateProfile(userId, requestDto);
	}

	//회원가입 시 프로필,메타데이터 디폴트로 생성.
	@Transactional
	public void createDefaultProfile(User user) {
		profileService.createDefaultProfile(user);
		metadataService.createDefaultMetadata(user);
	}
}
