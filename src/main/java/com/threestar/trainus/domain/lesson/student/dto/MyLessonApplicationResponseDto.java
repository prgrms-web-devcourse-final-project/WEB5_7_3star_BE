package com.threestar.trainus.domain.lesson.student.dto;

import java.time.LocalDateTime;

import lombok.Builder;

// 각 신청 항목 객체
@Builder
public record MyLessonApplicationResponseDto(
	Long lessonApplicationId,
	LessonSummaryResponseDto lesson,
	String status,
	LocalDateTime appliedAt
) {
}
