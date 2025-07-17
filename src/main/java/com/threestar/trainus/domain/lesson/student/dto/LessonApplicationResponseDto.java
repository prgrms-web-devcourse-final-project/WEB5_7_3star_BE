package com.threestar.trainus.domain.lesson.student.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.lesson.teacher.entity.ApplicationStatus;

import lombok.Builder;

@Builder
public record LessonApplicationResponseDto(
	Long lessonId,
	Long userId,
	ApplicationStatus status, // APPROVED, PENDING
	LocalDateTime appliedAt
) {
}
