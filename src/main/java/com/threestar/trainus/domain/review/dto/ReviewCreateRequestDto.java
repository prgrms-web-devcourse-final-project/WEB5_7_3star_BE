package com.threestar.trainus.domain.review.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewCreateRequestDto(
	String content,
	@NotNull(message = "점수는 필수입니다.")
	Double rating,
	String reviewImage
) {

}
