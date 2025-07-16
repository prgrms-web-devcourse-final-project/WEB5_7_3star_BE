package com.threestar.trainus.domain.comment.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentPageWrapperDto {
	private List<CommentResponseDto> comments;
}
