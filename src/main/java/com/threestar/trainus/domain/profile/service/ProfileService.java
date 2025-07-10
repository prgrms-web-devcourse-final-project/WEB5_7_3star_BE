package com.threestar.trainus.domain.profile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.profile.dto.ProfileResponseDto;
import com.threestar.trainus.domain.profile.dto.ProfileUpdateRequestDto;
import com.threestar.trainus.domain.profile.entity.Profile;
import com.threestar.trainus.domain.profile.mapper.ProfileMapper;
import com.threestar.trainus.domain.profile.repository.ProfileRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileService {

	private final ProfileRepository profileRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public ProfileResponseDto getProfile(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		Profile profile = profileRepository.findByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));

		return ProfileMapper.toResponseDto(profile, user);
	}

	@Transactional
	public ProfileResponseDto updateProfile(Long userId, ProfileUpdateRequestDto requestDto) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		Profile profile = profileRepository.findByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));

		Profile updatedProfile = ProfileMapper.toUpdateEntity(profile, requestDto);

		profileRepository.save(updatedProfile);

		return ProfileMapper.toResponseDto(updatedProfile, user);
	}

	public void createDefaultProfile(User user) {
		Profile defaultProfile = Profile.createDefault(user);
		profileRepository.save(defaultProfile);
	}
}

