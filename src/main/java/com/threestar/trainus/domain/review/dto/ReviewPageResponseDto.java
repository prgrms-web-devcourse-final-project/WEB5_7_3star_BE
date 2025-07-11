package com.threestar.trainus.domain.review.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewPageResponseDto {
	private Long userId;
	private Long count;
	private List<ReviewViewResponseDto> reviews;
}
