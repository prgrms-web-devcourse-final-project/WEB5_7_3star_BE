package com.threestar.trainus.domain.lesson.teacher.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonStatus;

import lombok.Builder;

/**
 * 레슨 수정 응답 데이터
 */
@Builder
public record LessonUpdateResponseDto(
	Long id,
	String lessonName,
	String description,
	Long lessonLeader,
	Category category,
	Integer price,
	Integer maxParticipants,
	LocalDateTime startAt,
	LocalDateTime endAt,
	LocalDateTime openTime,
	Boolean openRun,
	String city,
	String district,
	String dong,
	String ri,
	String address,
	String addressDetail,
	Double latitude,
	Double longitude,
	LessonStatus status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	List<String> lessonImages
) {
}
