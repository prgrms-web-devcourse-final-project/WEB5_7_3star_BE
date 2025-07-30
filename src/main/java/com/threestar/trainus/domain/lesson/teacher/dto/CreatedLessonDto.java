package com.threestar.trainus.domain.lesson.teacher.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.lesson.teacher.entity.LessonStatus;

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
	LessonStatus status,
	LocalDateTime startAt,
	LocalDateTime endAt,
	LocalDateTime openTime,
	Boolean openRun,
	String addressDetail
) {
}
