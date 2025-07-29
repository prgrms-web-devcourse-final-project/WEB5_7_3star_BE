package com.threestar.trainus.domain.comment.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record CommentPageResponseDto(
	List<CommentResponseDto> comments,
	Integer count
) {
}
