package com.threestar.trainus.domain.lesson.admin.dto;

import java.util.List;

import lombok.Builder;

/**
 * 개설한 레슨 목록 응답
 */
@Builder
public record CreatedLessonListResponseDto(
	List<CreatedLessonDto> lessons,
	Long count
) {
}
