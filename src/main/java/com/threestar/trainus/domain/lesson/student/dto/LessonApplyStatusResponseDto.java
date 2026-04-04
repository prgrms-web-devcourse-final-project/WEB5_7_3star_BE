package com.threestar.trainus.domain.lesson.student.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LessonApplyStatusResponseDto {
	private String status;
	private Long rank;
	private Long estimatedWaitTimeMs; // 선택 사항

	public static LessonApplyStatusResponseDto of(String status) {
		return LessonApplyStatusResponseDto.builder()
			.status(status)
			.build();
	}

	public static LessonApplyStatusResponseDto waiting(long rank) {
		return LessonApplyStatusResponseDto.builder()
			.status("WAITING")
			.rank(rank)
			.estimatedWaitTimeMs(rank * 100L) // 예: 인당 100ms 예상 시
			.build();
	}
}
