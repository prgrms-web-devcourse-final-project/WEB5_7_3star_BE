package com.threestar.trainus.domain.review.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewViewResponseDto {
	private Long reviewId;
	private Long lessonId;
	private String lessonName;
	private Long reviewerId;
	private String reviewerNickname;
	private String reviewImage;
	private String content;
	private Float rating;
	private LocalDateTime createdAt;
}
