package com.threestar.trainus.domain.review.dto;

import lombok.Builder;

@Builder
public record ReviewCreateResponseDto(
	Long reviewId,
	String content,
	Double rating,
	String reviewImage
) {
}
