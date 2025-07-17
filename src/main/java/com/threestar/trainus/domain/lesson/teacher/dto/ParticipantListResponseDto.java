package com.threestar.trainus.domain.lesson.teacher.dto;

import java.util.List;

import lombok.Builder;

/**
 * 참가자 목록 응답 데이터
 */
@Builder
public record ParticipantListResponseDto(
	List<ParticipantDto> lessonApplications,
	Integer count
) {
}
