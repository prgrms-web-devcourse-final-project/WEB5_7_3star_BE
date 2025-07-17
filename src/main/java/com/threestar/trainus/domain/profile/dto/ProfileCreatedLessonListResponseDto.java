package com.threestar.trainus.domain.profile.dto;

import java.util.List;

import lombok.Builder;

/**
 * 프로필에서 개설한 레슨 목록 응답
 */
@Builder
public record ProfileCreatedLessonListResponseDto(
	List<ProfileCreatedLessonDto> lessons,
	Integer count
) {
}
