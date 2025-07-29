package com.threestar.trainus.domain.review.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record ReviewPageResponseDto(
	Long userId,
	Integer count,
	List<ReviewViewResponseDto> reviews
) {
}
