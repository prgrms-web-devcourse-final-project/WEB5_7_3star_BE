package com.threestar.trainus.domain.review.dto;

import lombok.Data;

@Data
public class ReviewCreateRequestDto {
	private String content;
	private Float rating;
	private String reviewImage;
}
