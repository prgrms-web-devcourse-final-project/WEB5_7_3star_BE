package com.threestar.trainus.domain.comment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.comment.dto.CommentCreateRequestDto;
import com.threestar.trainus.domain.comment.dto.CommentPageResponseDto;
import com.threestar.trainus.domain.comment.dto.CommentResponseDto;
import com.threestar.trainus.domain.comment.service.CommentService;
import com.threestar.trainus.global.unit.BaseResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "댓글 API", description = "댓글 작성, 조회, 삭제 관련 API")
@RestController
@RequestMapping(("/api/v1/comments/"))
@RequiredArgsConstructor
public class CommentController {

	private final CommentService commentService;

	@PostMapping("{lessonId}")
	public ResponseEntity<BaseResponse<CommentResponseDto>> createComment(@PathVariable Long lessonId,
		@Valid @RequestBody CommentCreateRequestDto request, HttpSession session) {
		Long userId = (Long)session.getAttribute("LOGIN_USER");
		CommentResponseDto comment = commentService.createComment(request, lessonId, userId);
		return BaseResponse.ok("댓글 등록 완료되었습니다", comment, HttpStatus.CREATED);
	}

	@GetMapping("{lessonId}")
	public ResponseEntity<BaseResponse<CommentPageResponseDto>> readAll(@PathVariable Long lessonId,
		@RequestParam("page") int page,
		@RequestParam("pageSize") int pageSize) {
		return BaseResponse.ok("댓글 조회 성공", commentService.readAll(lessonId, page, pageSize), HttpStatus.OK);
	}

	@DeleteMapping("{commentId}")
	public ResponseEntity<BaseResponse<Void>> deleteComment(@PathVariable Long commentId, HttpSession session) {
		Long userId = (Long)session.getAttribute("LOGIN_USER");
		commentService.delete(commentId, userId);
		return BaseResponse.ok("댓글이 삭제되었습니다", null, HttpStatus.NO_CONTENT);
	}
}
