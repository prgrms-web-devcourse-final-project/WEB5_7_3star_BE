package com.threestar.trainus.domain.user.dto;

public record EmailVerificationResponseDto(
	String email,
	int expirationMinutes
) {
}
