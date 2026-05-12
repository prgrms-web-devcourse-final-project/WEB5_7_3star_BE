package com.threestar.trainus.domain.lesson.student.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record LessonApplyRequestResponseDto(
	Long lessonId,
	Long userId,
	String requestId,
	String status,
	LocalDateTime appliedAt
) {
}
