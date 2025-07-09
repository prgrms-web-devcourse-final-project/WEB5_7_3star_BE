package com.threestar.trainus.domain.lesson.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.threestar.trainus.domain.lesson.admin.entity.Category;

import lombok.Builder;

@Builder
public record LessonResponseDto(
	long id,
	String lessonName,
	String description,
	long lessonLeader,
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
	String status,
	LocalDateTime createdAt,
	List<String> lessonImages
) {
}