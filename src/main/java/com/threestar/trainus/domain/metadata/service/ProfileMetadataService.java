package com.threestar.trainus.domain.metadata.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.metadata.dto.ProfileMetadataResponseDto;
import com.threestar.trainus.domain.metadata.entity.ProfileMetadata;
import com.threestar.trainus.domain.metadata.mapper.ProfileMetadataMapper;
import com.threestar.trainus.domain.metadata.repositroy.ProfileMetadataRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileMetadataService {

	private final ProfileMetadataRepository profileMetadataRepository;
	private final UserRepository userRepository;

	@Transactional
	public void createDefaultMetadata(User user) {
		ProfileMetadata defaultMetadata = ProfileMetadataMapper.toDefaultEntity(user);
		profileMetadataRepository.save(defaultMetadata);
	}

	@Transactional(readOnly = true)
	public ProfileMetadataResponseDto getMetadata(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		ProfileMetadata profileMetadata = profileMetadataRepository.findByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.METADATA_NOT_FOUND));

		return ProfileMetadataMapper.toResponseDto(profileMetadata, user);
	}
}
