package com.threestar.trainus.domain.lesson.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 강사가 레슨 신청을 어떻게 처리할껀지 요청 -> 승인/거절
 */
public record ApplicationActionRequestDto(
	@NotBlank(message = "승인/거절을 선택해주세요.")
	String action
) {
}
