package com.threestar.trainus.domain.profile.dto;

public record IntroUpdateResponseDto(
	Long userId,
	String nickname,
	String intro
) {
}
