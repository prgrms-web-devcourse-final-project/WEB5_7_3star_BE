package com.threestar.trainus.domain.ranking.dto;

import com.threestar.trainus.domain.lesson.teacher.entity.Category;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RankingResponseDto {

	private Long userId;
	private String userNickname;
	private Double rating;
	private Integer reviewCount;
	private Category category;
	private Integer rank;
	private String profileImage;
}
