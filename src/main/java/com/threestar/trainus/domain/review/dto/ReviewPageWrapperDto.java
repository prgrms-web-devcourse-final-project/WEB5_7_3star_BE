package com.threestar.trainus.domain.review.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record ReviewPageWrapperDto(
	Long userId,
	List<ReviewViewResponseDto> reviews
) {
}
