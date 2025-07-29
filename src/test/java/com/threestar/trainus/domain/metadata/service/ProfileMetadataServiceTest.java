package com.threestar.trainus.domain.metadata.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.threestar.trainus.domain.metadata.dto.ProfileMetadataResponseDto;
import com.threestar.trainus.domain.metadata.entity.ProfileMetadata;
import com.threestar.trainus.domain.metadata.repository.ProfileMetadataRepository;
import com.threestar.trainus.domain.review.repository.ReviewRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

@ExtendWith(MockitoExtension.class)
class ProfileMetadataServiceTest {

	@InjectMocks
	private ProfileMetadataService profileMetadataService;

	@Mock
	private ProfileMetadataRepository profileMetadataRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ReviewRepository reviewRepository;

	@Nested
	@DisplayName("배치 메타데이터 업데이트 테스트")
	class BatchUpdateMetadataTest {

		@Test
		@DisplayName("성공 - 메타데이터 업데이트 필요한 경우")
		void batchUpdateMetadata_success_needsUpdate() {
			// given
			Long userId = 1L;
			User user = User.builder()
				.id(userId)
				.email("test@test.com")
				.nickname("테스트유저")
				.role(UserRole.USER)
				.build();

			ProfileMetadata metadata = ProfileMetadata.builder()
				.id(1L)
				.user(user)
				.reviewCount(0)
				.rating(0.0)
				.build();

			given(profileMetadataRepository.findByUserId(userId)).willReturn(Optional.of(metadata));
			given(reviewRepository.countByRevieweeId(userId)).willReturn(5);
			given(reviewRepository.findAverageRatingByRevieweeId(userId)).willReturn(4.2);

			// when
			profileMetadataService.batchUpdateMetadata(userId);

			// then
			verify(profileMetadataRepository).save(any(ProfileMetadata.class));
		}

		@Test
		@DisplayName("성공 - 업데이트 불필요한 경우")
		void batchUpdateMetadata_success_noUpdateNeeded() {
			// given
			Long userId = 1L;
			User user = User.builder()
				.id(userId)
				.email("test@test.com")
				.nickname("테스트유저")
				.role(UserRole.USER)
				.build();

			ProfileMetadata metadata = ProfileMetadata.builder()
				.id(1L)
				.user(user)
				.reviewCount(5)
				.rating(4.2)
				.build();

			given(profileMetadataRepository.findByUserId(userId)).willReturn(Optional.of(metadata));
			given(reviewRepository.countByRevieweeId(userId)).willReturn(5);
			given(reviewRepository.findAverageRatingByRevieweeId(userId)).willReturn(4.2);

			// when
			profileMetadataService.batchUpdateMetadata(userId);

			// then
			verify(profileMetadataRepository, never()).save(any());
		}

		@Test
		@DisplayName("실패 - 메타데이터 없음")
		void batchUpdateMetadata_fail_metadataNotFound() {
			// given
			Long userId = 1L;
			given(profileMetadataRepository.findByUserId(userId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> profileMetadataService.batchUpdateMetadata(userId))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.METADATA_NOT_FOUND);
		}

		@Test
		@DisplayName("성공 - 평점이 null인 경우 0.0으로 처리")
		void batchUpdateMetadata_success_nullRating() {
			// given
			Long userId = 1L;
			User user = User.builder()
				.id(userId)
				.email("test@test.com")
				.nickname("테스트유저")
				.role(UserRole.USER)
				.build();

			ProfileMetadata metadata = ProfileMetadata.builder()
				.id(1L)
				.user(user)
				.reviewCount(5)
				.rating(4.0)
				.build();

			given(profileMetadataRepository.findByUserId(userId)).willReturn(Optional.of(metadata));
			given(reviewRepository.countByRevieweeId(userId)).willReturn(0);
			given(reviewRepository.findAverageRatingByRevieweeId(userId)).willReturn(null);

			// when
			profileMetadataService.batchUpdateMetadata(userId);

			// then
			verify(profileMetadataRepository).save(any(ProfileMetadata.class));
		}
	}

	@Nested
	@DisplayName("메타데이터 조회 테스트")
	class GetMetadataTest {

		@Test
		@DisplayName("성공 - 메타데이터 조회")
		void getMetadata_success() {
			// given
			Long userId = 1L;
			User user = User.builder()
				.id(userId)
				.email("test@test.com")
				.nickname("테스트유저")
				.role(UserRole.USER)
				.build();

			ProfileMetadata metadata = ProfileMetadata.builder()
				.id(1L)
				.user(user)
				.reviewCount(10)
				.rating(4.5)
				.build();

			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(profileMetadataRepository.findByUserId(userId)).willReturn(Optional.of(metadata));

			// when
			ProfileMetadataResponseDto result = profileMetadataService.getMetadata(userId);

			// then
			assertThat(result).isNotNull();
		}

		@Test
		@DisplayName("실패 - 사용자 없음")
		void getMetadata_fail_userNotFound() {
			// given
			Long userId = 1L;
			given(userRepository.findById(userId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> profileMetadataService.getMetadata(userId))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
		}
	}
}