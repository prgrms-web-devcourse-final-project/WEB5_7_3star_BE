package com.threestar.trainus.domain.comment.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponseDto {
	private Long commentId;
	private Long userId;
	private String content;
	private Long parentCommentId;
	private Boolean deleted;
	private LocalDateTime createdAt;
}
