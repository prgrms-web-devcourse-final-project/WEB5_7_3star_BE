package com.threestar.trainus.domain.comment.dto;

import lombok.Data;

@Data
public class CommentCreateRequestDto {
	private String content;
	private Long parentCommentId;
}
