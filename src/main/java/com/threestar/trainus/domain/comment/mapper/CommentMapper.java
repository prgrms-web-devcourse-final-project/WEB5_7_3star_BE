package com.threestar.trainus.domain.comment.mapper;

import java.util.List;

import com.threestar.trainus.domain.comment.dto.CommentPageResponseDto;
import com.threestar.trainus.domain.comment.dto.CommentResponseDto;
import com.threestar.trainus.domain.comment.entity.Comment;

public class CommentMapper {

	private CommentMapper() {
	}

	public static CommentResponseDto toCommentResponseDto(Comment comment) {
		return CommentResponseDto.builder()
			.commentId(comment.getCommentId())
			.userId(comment.getUser().getId())
			.content(comment.getContent())
			.parentCommentId(comment.getParentCommentId())
			.deleted(comment.getDeleted())
			.createdAt(comment.getCreatedAt())
			.build();
	}

	public static CommentPageResponseDto toCommentPageResponseDto(List<CommentResponseDto> comments, Long count) {
		return CommentPageResponseDto.builder()
			.comments(comments)
			.count(count)
			.build();
	}
}
