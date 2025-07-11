package com.threestar.trainus.domain.review.mapper;

import java.util.List;

import com.threestar.trainus.domain.review.dto.ReviewCreateResponseDto;
import com.threestar.trainus.domain.review.dto.ReviewPageResponseDto;
import com.threestar.trainus.domain.review.dto.ReviewViewResponseDto;
import com.threestar.trainus.domain.review.entity.Review;

public class ReviewMapper {

	private ReviewMapper() {
	}

	public static ReviewCreateResponseDto toReviewResponseDto(Review review) {
		return ReviewCreateResponseDto.builder()
			.reviewId(review.getReviewId())
			.content(review.getContent())
			.rating(review.getRating())
			.reviewImage(review.getImage())
			.build();
	}

	public static ReviewViewResponseDto toReviewViewResponseDto(Review review) {
		return ReviewViewResponseDto.builder()
			.reviewId(review.getReviewId())
			.lessonId(review.getLesson().getId())
			.lessonName(review.getLesson().getLessonName())
			.reviewerId(review.getReviewer().getId())
			.reviewerNickname(review.getReviewer().getNickname())
			.reviewImage(review.getImage())
			.content(review.getContent())
			.rating(review.getRating())
			.createdAt(review.getCreatedAt())
			.build();
	}

	public static ReviewPageResponseDto toReviewPageResponseDto(Long userId, List<ReviewViewResponseDto> reviews,
		Long count) {
		return ReviewPageResponseDto.builder()
			.reviews(reviews)
			.userId(userId)
			.count(count)
			.build();
	}
}
