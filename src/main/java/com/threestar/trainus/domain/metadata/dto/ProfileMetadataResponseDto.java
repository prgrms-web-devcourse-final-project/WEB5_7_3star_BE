package com.threestar.trainus.domain.metadata.dto;

public record ProfileMetadataResponseDto(
	Long userId,
	Integer reviewCount,
	Float rating
) {
}
