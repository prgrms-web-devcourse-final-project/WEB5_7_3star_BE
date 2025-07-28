package com.threestar.trainus.domain.lesson.teacher.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonStatus;

import lombok.Builder;

@Builder
public record LessonResponseDto(
	Long id,
	String lessonName,
	String description,
	Long lessonLeader,
	Category category,
	int price,
	int maxParticipants,
	LocalDateTime startAt,
	LocalDateTime endAt,
	LocalDateTime openTime,
	boolean openRun,
	String city,
	String district,
	String dong,
	String addressDetail,
	LessonStatus status,
	LocalDateTime createdAt,
	List<String> lessonImages
) {
}
