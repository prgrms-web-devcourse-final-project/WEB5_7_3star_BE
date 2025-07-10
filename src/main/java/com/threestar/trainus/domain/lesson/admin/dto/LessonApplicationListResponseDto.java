package com.threestar.trainus.domain.lesson.admin.dto;

import lombok.Builder;

/**
 * 레슨 신청자 목록과 총 개수를 담는 응답데이터
 */
@Builder
public record LessonApplicationListResponseDto(
	java.util.List<LessonApplicationResponseDto> lessonApplications,
	Long totalCount
) {
}
