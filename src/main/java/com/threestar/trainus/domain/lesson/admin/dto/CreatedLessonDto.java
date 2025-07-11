package com.threestar.trainus.domain.lesson.admin.dto;

import java.time.LocalDateTime;

import lombok.Builder;

/**
 * 개설한 레슨 정보
 */
@Builder
public record CreatedLessonDto(
	Long id,
	String lessonName,
	Integer maxParticipants,
	Integer currentParticipants,
	Integer price,
	String status,
	LocalDateTime startAt,
	LocalDateTime endAt,
	Boolean openRun,
	String addressDetail
) {
}
