package com.threestar.trainus.domain.user.dto;

import com.threestar.trainus.domain.user.entity.UserRole;

public record UserInfoResponseDto(
	Long userId,
	String nickname,
	String email,
	UserRole role
) {
}
