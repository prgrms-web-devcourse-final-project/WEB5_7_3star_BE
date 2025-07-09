package com.threestar.trainus.domain.user.dto;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.user.entity.UserRole;

public record SignupResponseDto(
	Long id,
	String email,
	String nickname,
	UserRole userRole,
	LocalDateTime createAt
) {
}
