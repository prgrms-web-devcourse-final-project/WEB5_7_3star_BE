package com.threestar.trainus.domain.lesson.admin.dto;

import java.time.LocalDateTime;

import lombok.Builder;

/**
 * 레슨 신청 처리결과를 강사에게 알려주는 응답데이터
 */
@Builder
public record ApplicationProcessDto(
	Long lessonApplicationId,
	Long userId,
	String status,
	LocalDateTime processedAt
) {
}
