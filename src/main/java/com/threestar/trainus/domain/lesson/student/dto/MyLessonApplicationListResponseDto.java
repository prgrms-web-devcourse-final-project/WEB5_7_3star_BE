package com.threestar.trainus.domain.lesson.student.dto;

import java.util.List;

import lombok.Builder;

@Builder
// 내 신청 현황 List 전체 응답
public record MyLessonApplicationListResponseDto(
	List<MyLessonApplicationResponseDto> lessonApplications,
	int count
) {
}
