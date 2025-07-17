package com.threestar.trainus.domain.lesson.teacher.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.lesson.teacher.entity.ApplicationStatus;

import lombok.Builder;

/**
 * 레슨 신청 처리결과를 강사에게 알려주는 응답데이터
 */
@Builder
public record ApplicationProcessResponseDto(
	Long lessonApplicationId,
	Long userId,
	ApplicationStatus status,
	LocalDateTime processedAt
) {
}
