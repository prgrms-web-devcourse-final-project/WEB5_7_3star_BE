package com.threestar.trainus.domain.lesson.admin.dto;

import lombok.Builder;

@Builder
public record LessonApplicationListResponseDto(
	java.util.List<LessonApplicationResponseDto> lessonApplications,
	Long totalCount
) {
}
