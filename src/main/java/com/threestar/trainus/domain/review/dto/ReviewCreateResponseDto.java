package com.threestar.trainus.domain.review.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewCreateResponseDto {
	private Long reviewId;
	private String content;
	private Float rating;
	private String reviewImage;
}
