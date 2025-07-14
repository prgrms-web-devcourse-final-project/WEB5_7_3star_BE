package com.threestar.trainus.domain.lesson.student.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record LessonApplicationResponseDto(
	Long lessonApplicationId,
	Long lessonId,
	Long userId,
	String status, // APPROVED, PENDING
	LocalDateTime appliedAt
) {}
