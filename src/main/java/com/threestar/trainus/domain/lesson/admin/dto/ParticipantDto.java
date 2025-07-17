package com.threestar.trainus.domain.lesson.admin.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.profile.dto.ProfileResponseDto;

import lombok.Builder;

//개별 참가자 정보
@Builder
public record ParticipantDto(
	Long lessonApplicationId,
	ProfileResponseDto user,
	LocalDateTime joinedAt
) {
}
