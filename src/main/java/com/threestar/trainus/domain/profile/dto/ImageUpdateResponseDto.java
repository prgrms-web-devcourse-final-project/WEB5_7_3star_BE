package com.threestar.trainus.domain.profile.dto;

public record ImageUpdateResponseDto(
	Long userId,
	String nickname,
	String profileImage
) {
}
