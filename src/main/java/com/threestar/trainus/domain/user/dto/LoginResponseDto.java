package com.threestar.trainus.domain.user.dto;

import com.threestar.trainus.domain.user.entity.UserRole;

public record LoginResponseDto(
	Long id,
	String email,
	String nickname,
	UserRole role
) {
}
