package com.threestar.trainus.domain.metadata.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.metadata.dto.ProfileMetadataResponseDto;
import com.threestar.trainus.domain.metadata.entity.ProfileMetadata;
import com.threestar.trainus.domain.metadata.mapper.ProfileMetadataMapper;
import com.threestar.trainus.domain.metadata.repository.ProfileMetadataRepository;
import com.threestar.trainus.domain.review.repository.ReviewRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileMetadataService {

	private final ProfileMetadataRepository profileMetadataRepository;
	private final UserRepository userRepository;
	private final ReviewRepository reviewRepository;

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

	// @Transactional
	// public void increaseReviewCountAndRating(Long userId, double newRating) {
	// 	ProfileMetadata profileMetadata = profileMetadataRepository.findWithLockByUserId(userId)
	// 		.orElseThrow(() -> new BusinessException(ErrorCode.METADATA_NOT_FOUND));
	// 	profileMetadata.increaseReviewCount();
	// 	profileMetadata.setRating(profileMetadata.updateRating(newRating));
	// }

	@Transactional
	public void batchUpdateMetadata(Long userId) {
		log.debug("사용자 ID {}의 메타데이터 배치 업데이트 시작", userId);

		ProfileMetadata profileMetadata = profileMetadataRepository.findByUserId(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.METADATA_NOT_FOUND));

		// 실제 리뷰 수와 평균 평점 계산
		Integer actualReviewCount = reviewRepository.countByRevieweeId(userId);
		Double actualAverageRating = reviewRepository.findAverageRatingByRevieweeId(userId);

		if (actualAverageRating == null) {
			actualAverageRating = 0.0;
		}

		// 현재 값과 다르면 업데이트
		boolean updated = false;
		if (!profileMetadata.getReviewCount().equals(actualReviewCount)) {
			profileMetadata = ProfileMetadata.builder()
				.id(profileMetadata.getId())
				.user(profileMetadata.getUser())
				.reviewCount(actualReviewCount)
				.rating(actualAverageRating)
				.build();
			updated = true;
		} else if (!profileMetadata.getRating().equals(actualAverageRating)) {
			profileMetadata.setRating(actualAverageRating);
			updated = true;
		}

		if (updated) {
			profileMetadataRepository.save(profileMetadata);
			log.debug("사용자 ID {}의 메타데이터 업데이트 완료. 리뷰수: {}, 평점: {}",
				userId, actualReviewCount, actualAverageRating);
		}
	}
}
