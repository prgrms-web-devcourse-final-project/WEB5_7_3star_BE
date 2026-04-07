package com.threestar.trainus.domain.lesson.student.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LessonApplyStatusResponseDto {
	private String status;
	private Long rank;
	private Long estimatedWaitTimeMs;

	public static LessonApplyStatusResponseDto of(String status) {
		return LessonApplyStatusResponseDto.builder()
			.status(status)
			.build();
	}

	public static LessonApplyStatusResponseDto waiting(long rank) {
		return LessonApplyStatusResponseDto.builder()
			.status("WAITING")
			.rank(rank)
			.estimatedWaitTimeMs(rank * 100L)
			.build();
	}
}
