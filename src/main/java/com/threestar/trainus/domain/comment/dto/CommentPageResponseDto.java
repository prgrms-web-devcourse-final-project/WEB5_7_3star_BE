package com.threestar.trainus.domain.comment.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentPageResponseDto {

	private List<CommentResponseDto> comments;
	private Long count;
}
