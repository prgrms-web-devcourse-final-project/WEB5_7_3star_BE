package com.threestar.trainus.domain.user.dto;

public record EmailSendResponseDto(
	String email,
	int expirationMinutes
) {
}
