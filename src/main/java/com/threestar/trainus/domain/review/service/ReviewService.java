package com.threestar.trainus.domain.review.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.lesson.admin.repository.LessonParticipantRepository;
import com.threestar.trainus.domain.lesson.admin.repository.LessonRepository;
import com.threestar.trainus.domain.metadata.entity.ProfileMetadata;
import com.threestar.trainus.domain.metadata.repository.ProfileMetadataRepository;
import com.threestar.trainus.domain.review.dto.ReviewCreateRequestDto;
import com.threestar.trainus.domain.review.dto.ReviewCreateResponseDto;
import com.threestar.trainus.domain.review.dto.ReviewPageResponseDto;
import com.threestar.trainus.domain.review.entity.Review;
import com.threestar.trainus.domain.review.mapper.ReviewMapper;
import com.threestar.trainus.domain.review.repository.ReviewRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;
import com.threestar.trainus.global.utils.PageLimitCalculator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

	private final ReviewRepository reviewRepository;
	private final LessonRepository lessonRepository;
	private final UserRepository userRepository;
	private final ProfileMetadataRepository profileMetadataRepository;
	private final LessonParticipantRepository lessonParticipantRepository;

	//참여자 테이블에 있는지도 검증 필요 횟수도 한번으로 제한

	@Transactional
	public ReviewCreateResponseDto createReview(ReviewCreateRequestDto reviewRequestDto, Long lessonId, Long userId) {
		Lesson findLesson = lessonRepository.findById(lessonId)
			.orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));

		LocalDateTime reviewEndDate = findLesson.getEndAt().plusDays(7);

		if (LocalDateTime.now().isBefore(findLesson.getEndAt()) && LocalDateTime.now().isAfter(reviewEndDate)) {
			throw new BusinessException(ErrorCode.INVALID_REVIEW_DATE);
		}

		User findUser = userRepository.findById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		User lessonLeader = userRepository.findById(findLesson.getLessonLeader())
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		//참여자 테이블 검증 추후 추가 -> lessonId 와 userId 다 갖고 있는지
		if (!lessonParticipantRepository.existsByLessonIdAndUserId(findLesson.getId(),
			findUser.getId())) {
			throw new BusinessException(ErrorCode.INVALID_LESSON_PARTICIPANT);
		}
		if (reviewRepository.existsByReviewer_IdAndLessonId(findUser.getId(), findLesson.getId())) {
			throw new BusinessException(ErrorCode.INVALID_REVIEW_COUNT);
		}

		Review newReview = reviewRepository.save(Review.builder()
			.reviewer(findUser)
			.reviewee(lessonLeader)
			.lesson(findLesson)
			.content(reviewRequestDto.getContent())
			.rating(reviewRequestDto.getRating())
			.image(reviewRequestDto.getReviewImage())
			.build());

		ProfileMetadata profileMetadata = profileMetadataRepository.findWithLockByUserId(
				lessonLeader.getId())  //비관적 락 적용
			.orElseThrow(() -> new BusinessException(ErrorCode.METADATA_NOT_FOUND));

		profileMetadata.increaseReviewCount();
		profileMetadata.setRating(profileMetadata.updateRating(reviewRequestDto.getRating()));

		return ReviewMapper.toReviewResponseDto(newReview);
	}

	@Transactional(readOnly = true)
	public ReviewPageResponseDto readAll(Long userId, int page, int pageSize) {
		return ReviewMapper.toReviewPageResponseDto(userId,
			reviewRepository.findByReviewee_Id(userId).stream()
				.map(ReviewMapper::toReviewViewResponseDto)
				.toList(),
			reviewRepository.count(userId, PageLimitCalculator.calculatePageLimit(page, pageSize, 5))
		);
	}
}
