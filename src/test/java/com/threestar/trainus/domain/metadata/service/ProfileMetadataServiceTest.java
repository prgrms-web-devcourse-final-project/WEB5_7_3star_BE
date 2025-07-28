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
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileMetadataService 테스트")
class ProfileMetadataServiceTest {

	@InjectMocks
	private ProfileMetadataService profileMetadataService;

	@Mock
	private ProfileMetadataRepository profileMetadataRepository;

	@Mock
	private UserRepository userRepository;

	@Nested
	@DisplayName("기본 메타데이터 생성")
	class CreateDefaultMetadataTest {

		@Test
		@DisplayName("성공 - 기본 메타데이터 생성")
		void createDefaultMetadata_success() {
			// given
			User user = User.builder()
				.id(1L)
				.email("test@email.com")
				.nickname("testUser")
				.role(UserRole.USER)
				.build();

			ProfileMetadata savedMetadata = ProfileMetadata.builder()
				.id(1L)
				.user(user)
				.reviewCount(0)
				.rating(0.0)
				.build();

			given(profileMetadataRepository.save(any(ProfileMetadata.class))).willReturn(savedMetadata);

			// when
			profileMetadataService.createDefaultMetadata(user);

			// then
			verify(profileMetadataRepository).save(argThat(metadata ->
				metadata.getUser().equals(user) &&
					metadata.getReviewCount().equals(0) &&
					metadata.getRating().equals(0.0)
			));
		}
	}

	@Nested
	@DisplayName("메타데이터 조회")
	class GetMetadataTest {

		@Test
		@DisplayName("성공 - 메타데이터 조회")
		void getMetadata_success() {
			// given
			Long userId = 1L;
			User user = User.builder()
				.id(userId)
				.email("test@email.com")
				.nickname("testUser")
				.build();

			ProfileMetadata metadata = ProfileMetadata.builder()
				.id(1L)
				.user(user)
				.reviewCount(5)
				.rating(4.5)
				.build();

			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(profileMetadataRepository.findByUserId(userId)).willReturn(Optional.of(metadata));

			// when
			ProfileMetadataResponseDto result = profileMetadataService.getMetadata(userId);

			// then
			assertThat(result).isNotNull();
			assertThat(result.userId()).isEqualTo(userId);
			assertThat(result.reviewCount()).isEqualTo(5);
			assertThat(result.rating()).isEqualTo(4.5);
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

		@Test
		@DisplayName("실패 - 메타데이터 없음")
		void getMetadata_fail_metadataNotFound() {
			// given
			Long userId = 1L;
			User user = User.builder().id(userId).build();

			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(profileMetadataRepository.findByUserId(userId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> profileMetadataService.getMetadata(userId))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.METADATA_NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("리뷰 카운트 증가 및 평점 업데이트")
	class IncreaseReviewCountAndRatingTest {

		@Test
		@DisplayName("성공 - 첫 번째 리뷰 (기존 평점 0.0)")
		void increaseReviewCountAndRating_success_firstReview() {
			// given
			Long userId = 1L;
			double newRating = 4.5;

			User user = User.builder().id(userId).build();
			ProfileMetadata metadata = ProfileMetadata.builder()
				.id(1L)
				.user(user)
				.reviewCount(0)
				.rating(0.0)
				.build();

			given(profileMetadataRepository.findWithLockByUserId(userId)).willReturn(Optional.of(metadata));

			// when
			profileMetadataService.increaseReviewCountAndRating(userId, newRating);

			// then
			assertThat(metadata.getReviewCount()).isEqualTo(1);

			assertThat(metadata.getRating()).isEqualTo(4.5);
		}

		@Test
		@DisplayName("성공 - 기존 리뷰가 있는 경우 평점 계산")
		void increaseReviewCountAndRating_success_existingReviews() {
			// given
			Long userId = 1L;
			double newRating = 3.0;

			User user = User.builder().id(userId).build();
			ProfileMetadata metadata = ProfileMetadata.builder()
				.id(1L)
				.user(user)
				.reviewCount(2) // 기존 리뷰 2개
				.rating(4.0)    // 기존 평균 평점 4.0
				.build();

			given(profileMetadataRepository.findWithLockByUserId(userId)).willReturn(Optional.of(metadata));

			// when
			profileMetadataService.increaseReviewCountAndRating(userId, newRating);

			// then
			assertThat(metadata.getReviewCount()).isEqualTo(3);

			assertThat(metadata.getRating()).isCloseTo(3.67, within(0.01));
		}

		@Test
		@DisplayName("성공 - 정확한 평점 계산 검증")
		void increaseReviewCountAndRating_success_exactCalculation() {
			// given
			Long userId = 1L;
			double newRating = 5.0;

			User user = User.builder().id(userId).build();
			ProfileMetadata metadata = ProfileMetadata.builder()
				.id(1L)
				.user(user)
				.reviewCount(3) // 기존 리뷰 3개
				.rating(3.0)    // 기존 평균 평점 3.0
				.build();

			given(profileMetadataRepository.findWithLockByUserId(userId)).willReturn(Optional.of(metadata));

			// when
			profileMetadataService.increaseReviewCountAndRating(userId, newRating);

			// then
			assertThat(metadata.getReviewCount()).isEqualTo(4);

			assertThat(metadata.getRating()).isEqualTo(3.5);
		}

		@Test
		@DisplayName("실패 - 메타데이터 없음 (락 조회)")
		void increaseReviewCountAndRating_fail_metadataNotFound() {
			// given
			Long userId = 1L;
			double newRating = 4.5;

			given(profileMetadataRepository.findWithLockByUserId(userId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> profileMetadataService.increaseReviewCountAndRating(userId, newRating))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.METADATA_NOT_FOUND);
		}

		@Test
		@DisplayName("동시성 테스트 - 락 사용 확인")
		void increaseReviewCountAndRating_concurrency_lockUsage() {
			// given
			Long userId = 1L;
			double newRating = 4.0;

			User user = User.builder().id(userId).build();
			ProfileMetadata metadata = ProfileMetadata.builder()
				.id(1L)
				.user(user)
				.reviewCount(1)
				.rating(3.0)
				.build();

			given(profileMetadataRepository.findWithLockByUserId(userId)).willReturn(Optional.of(metadata));

			// when
			profileMetadataService.increaseReviewCountAndRating(userId, newRating);

			// then
			verify(profileMetadataRepository).findWithLockByUserId(userId);
			verify(profileMetadataRepository, never()).findByUserId(userId);
		}
	}
}