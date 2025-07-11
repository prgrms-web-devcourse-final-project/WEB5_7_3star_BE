package com.threestar.trainus.domain.lesson.admin.dto;

import java.time.LocalDateTime;

import lombok.Builder;

//개별 참가자 정보
@Builder
public record ParticipantDto(
	Long lessonApplicationId,
	UserSimpleDto user,
	LocalDateTime joinedAt
) {
}
