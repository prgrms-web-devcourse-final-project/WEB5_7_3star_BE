package com.threestar.trainus.domain.lesson.student.dto;

import java.time.LocalDateTime;

import lombok.Builder;

// 레슨 요약 객체
@Builder
public record LessonSummaryResponseDto(
	Long id,
	String lessonName,
	Long lessonLeader,
	LocalDateTime startAt,
	Integer price,
	String addressDetail
) {
}
