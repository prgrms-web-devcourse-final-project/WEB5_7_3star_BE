package com.threestar.trainus.domain.profile.dto;

public record ProfileDetailResponseDto(
	Long userId,
	String nickname,
	String profileImage,
	String intro,
	Integer reviewCount,
	Float rating
) {
}
