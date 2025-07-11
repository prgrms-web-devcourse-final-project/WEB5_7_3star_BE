package com.threestar.trainus.domain.profile.dto;

public record ProfileResponseDto(
	Long userId,
	String nickname,
	String profileImage,
	String intro
) {
}
