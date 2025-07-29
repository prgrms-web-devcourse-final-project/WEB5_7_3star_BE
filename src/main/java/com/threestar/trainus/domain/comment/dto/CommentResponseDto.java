package com.threestar.trainus.domain.comment.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record CommentResponseDto(
	Long commentId,
	Long userId,
	String nickname,
	String content,
	Long parentCommentId,
	Boolean deleted,
	LocalDateTime createdAt
) {
}
