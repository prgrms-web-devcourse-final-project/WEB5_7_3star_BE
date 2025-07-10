package com.threestar.trainus.domain.lesson.admin.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record LessonApplicationResponseDto(
	Long lessonApplicationId,
	UserSimpleDto user, //TODO: profile 도메인 완성 후 ProfileDto로 변경 예정
	String status,
	LocalDateTime appliedAt
) {
}

