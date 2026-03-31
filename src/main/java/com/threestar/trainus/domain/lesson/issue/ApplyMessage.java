package com.threestar.trainus.domain.lesson.issue;

import lombok.Builder;

//Redis Stream 메세지 배치 처리 DTO
@Builder
public record ApplyMessage(
	Long lessonId,
	Long userId,
	String requestId,
	Long timestamp
) {
}
