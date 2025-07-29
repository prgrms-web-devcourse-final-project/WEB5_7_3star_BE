package com.threestar.trainus.domain.comment.mapper;

import java.util.List;

import com.threestar.trainus.domain.comment.dto.CommentPageResponseDto;
import com.threestar.trainus.domain.comment.dto.CommentResponseDto;
import com.threestar.trainus.domain.comment.dto.CommentWithUserProjection;
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
			.nickname(comment.getUser().getNickname())
			.build();
	}

	public static CommentResponseDto toCommentResponseDtoWithProjection(CommentWithUserProjection comment) {
		return CommentResponseDto.builder()
			.commentId(comment.getCommentId())
			.userId(comment.getUserId())
			.content(comment.getContent())
			.parentCommentId(comment.getParentCommentId())
			.deleted(comment.getDeleted())
			.createdAt(comment.getCreatedAt())
			.nickname(comment.getNickname())
			.build();
	}

	public static CommentPageResponseDto toCommentPageResponseDto(List<CommentResponseDto> comments, Integer count) {
		return CommentPageResponseDto.builder()
			.comments(comments)
			.count(count)
			.build();
	}
}
