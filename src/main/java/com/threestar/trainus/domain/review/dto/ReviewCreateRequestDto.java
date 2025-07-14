package com.threestar.trainus.domain.review.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewCreateRequestDto {
	private String content;
	@NotNull(message = "점수는 필수입니다.")
	private Double rating;
	private String reviewImage;
}
