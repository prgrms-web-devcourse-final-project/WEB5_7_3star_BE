package com.threestar.trainus.domain.comment.dto;

import lombok.Getter;

@Getter
public class CommentCreateRequestDto {
	private String content;
	private Long parentCommentId;
}
