package com.threestar.trainus.domain.review.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record ReviewViewResponseDto(
	Long reviewId,
	Long lessonId,
	String lessonName,
	Long reviewerId,
	String reviewerNickname,
	String reviewImage,
	String content,
	Double rating,
	LocalDateTime createdAt
) {
}
