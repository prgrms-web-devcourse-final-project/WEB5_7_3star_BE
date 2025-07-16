package com.threestar.trainus.domain.review.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewPageWrapperDto {
	private Long userId;
	private List<ReviewViewResponseDto> reviews;
}
