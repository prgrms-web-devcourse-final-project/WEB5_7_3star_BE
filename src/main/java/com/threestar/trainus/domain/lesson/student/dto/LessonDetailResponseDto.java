package com.threestar.trainus.domain.lesson.student.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;

@Builder
public record LessonDetailResponseDto(
	Long id,
	String lessonName,
	String description,
	Long lessonLeader,
	String lessonLeaderName,
	String profileIntro,
	String profileImage,
	int likeCount,
	int reviewCount,
	float rating,
	String category,
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
	String addressDetail,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	List<String> lessonImages
) {}