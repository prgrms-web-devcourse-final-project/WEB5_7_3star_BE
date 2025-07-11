package com.threestar.trainus.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentCreateRequestDto {
	@NotBlank(message = "댓글 내용은 필수입니다")
	@Size(max = 50, message = "댓글은 50자 이내여야 합니다.")
	private String content;
	private Long parentCommentId;
}
