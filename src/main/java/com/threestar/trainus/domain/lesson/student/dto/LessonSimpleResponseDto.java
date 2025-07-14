package com.threestar.trainus.domain.lesson.student.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record LessonSimpleResponseDto(
	Long lessonId,
	String lessonName,
	LocalDateTime startAt,
	LocalDateTime endAt,
	Long price,
	String addressDetail
) {
}
