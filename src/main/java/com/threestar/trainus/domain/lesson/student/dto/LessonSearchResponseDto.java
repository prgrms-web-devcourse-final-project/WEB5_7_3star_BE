package com.threestar.trainus.domain.lesson.student.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.threestar.trainus.domain.lesson.admin.entity.Category;

public record LessonSearchResponseDto(
	long id,
	String lessonName,
	String lessonLeaderName,
	String lessonLeaderImage,
	int likeCount,
	int reviewCount,
	float rating,
	Category category,
	int price,
	int maxParticipants,
	int currentParticipants,
	String status,
	LocalDateTime startAt,
	LocalDateTime endAt,
	LocalDateTime openTime,
	boolean openRun,
	String city,
	String district,
	String dong,
	LocalDateTime createdAt,
	List<String> lessonImages
) {

}
