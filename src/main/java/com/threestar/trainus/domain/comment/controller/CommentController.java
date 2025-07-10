package com.threestar.trainus.domain.comment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.comment.dto.CommentCreateRequestDto;
import com.threestar.trainus.domain.comment.dto.CommentPageResponseDto;
import com.threestar.trainus.domain.comment.dto.CommentResponseDto;
import com.threestar.trainus.domain.comment.service.CommentService;
import com.threestar.trainus.global.unit.BaseResponse;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CommentController {

	private final CommentService commentService;

	@PostMapping("/api/v1/comments/{lessonId}")
	public ResponseEntity<BaseResponse<CommentResponseDto>> createComment(@PathVariable Long lessonId,
		@RequestBody CommentCreateRequestDto request, HttpSession session) {
		Long userId = (Long)session.getAttribute("LOGIN_USER");
		CommentResponseDto comment = commentService.createComment(request, lessonId, userId);
		return BaseResponse.ok("댓글 등록 완료되었습니다", comment, HttpStatus.CREATED);
	}

	@GetMapping("/api/v1/comments/{lessonId}")
	public ResponseEntity<BaseResponse<CommentPageResponseDto>> readAll(@PathVariable Long lessonId,
		@RequestParam("page") Long page,
		@RequestParam("pageSize") Long pageSize) {
		return BaseResponse.ok("댓글 조회 성공", commentService.readAll(lessonId, page, pageSize), HttpStatus.OK);
	}
}
