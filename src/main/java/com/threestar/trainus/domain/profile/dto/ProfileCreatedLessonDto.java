package com.threestar.trainus.domain.profile.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.lesson.teacher.entity.LessonStatus;

import lombok.Builder;

/**
 * 프로필에서 보여줄 개설한 레슨 정보
 */
@Builder
public record ProfileCreatedLessonDto(
	Long id,
	String lessonName,
	Integer maxParticipants,
	Integer currentParticipants,
	Integer price,
	LessonStatus status,
	LocalDateTime startAt,
	LocalDateTime endAt,
	Boolean openRun,
	String addressDetail
) {
}
