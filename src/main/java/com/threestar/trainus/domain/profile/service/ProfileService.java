package com.threestar.trainus.domain.profile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.profile.dto.IntroUpdateRequestDto;
import com.threestar.trainus.domain.profile.dto.ImageUpdateResponseDto;
import com.threestar.trainus.domain.profile.dto.ImageUpdateRequestDto;
import com.threestar.trainus.domain.profile.dto.IntroUpdateResponseDto;
import com.threestar.trainus.domain.profile.dto.ProfileResponseDto;
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

	@Transactional
	public void createDefaultProfile(User user) {
		Profile defaultProfile = ProfileMapper.toDefaultEntity(user);
		profileRepository.save(defaultProfile);
	}

	@Transactional(readOnly = true)
	public ProfileResponseDto getProfile(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		Profile profile = findByUserId(userId);

		return ProfileMapper.toResponseDto(profile, user);
	}

	@Transactional
	public ImageUpdateResponseDto updateProfileImage(Long userId, ImageUpdateRequestDto requestDto) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		Profile profile = findByUserId(userId);

		profile.updateProfileImage(requestDto.profileImage());

		return ProfileMapper.toImageResponseDto(profile, user);
	}

	@Transactional
	public IntroUpdateResponseDto updateProfileIntro(Long userId, IntroUpdateRequestDto requestDto) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		Profile profile = profileRepository.findByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));

		profile.updateProfileIntro(requestDto.intro());

		return ProfileMapper.toIntroResponseDto(profile, user);
	}

	public Profile findByUserId(Long userId) {
		return profileRepository.findByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
	}
}

