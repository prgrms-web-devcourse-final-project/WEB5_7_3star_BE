package com.threestar.trainus.domain.comment.dto;

import java.time.LocalDateTime;

public interface CommentWithUserProjection {
	Long getCommentId();

	String getContent();

	Long getParentCommentId();

	Boolean getDeleted();

	LocalDateTime getCreatedAt();

	Long getUserId();

	String getNickname();
}
