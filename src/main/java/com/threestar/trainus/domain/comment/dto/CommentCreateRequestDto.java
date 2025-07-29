package com.threestar.trainus.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequestDto(
	@NotBlank(message = "댓글 내용은 필수입니다")
	@Size(max = 255, message = "댓글은 255자 이내여야 합니다.")
	String content,
	Long parentCommentId
) {
}
