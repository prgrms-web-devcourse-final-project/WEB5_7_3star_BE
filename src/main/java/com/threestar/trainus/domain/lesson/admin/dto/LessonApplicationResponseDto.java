package com.threestar.trainus.domain.lesson.admin.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.profile.dto.ProfileResponseDto;

import lombok.Builder;

/**
 * 개별 레슨 신청자 정보를 담는 데이터
 */
@Builder
public record LessonApplicationResponseDto(
	Long lessonApplicationId,
	ProfileResponseDto user,
	String status,
	LocalDateTime appliedAt
) {
}

