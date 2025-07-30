package com.threestar.trainus.domain.comment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.comment.dto.CommentCreateRequestDto;
import com.threestar.trainus.domain.comment.dto.CommentPageResponseDto;
import com.threestar.trainus.domain.comment.dto.CommentResponseDto;
import com.threestar.trainus.domain.comment.service.CommentService;
import com.threestar.trainus.global.annotation.LoginUser;
import com.threestar.trainus.global.dto.PageRequestDto;
import com.threestar.trainus.global.unit.BaseResponse;
import com.threestar.trainus.global.unit.PagedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "댓글 API", description = "댓글 작성, 조회, 삭제 관련 API")
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

	private final CommentService commentService;

	@PostMapping("/{lessonId}")
	@Operation(summary = "댓글 작성", description = "레슨 ID에 해당되는 댓글을 작성합니다.")
	public ResponseEntity<BaseResponse<CommentResponseDto>> createComment(@PathVariable Long lessonId,
		@Valid @RequestBody CommentCreateRequestDto request, @LoginUser Long userId) {
		CommentResponseDto comment = commentService.createComment(request, lessonId, userId);
		return BaseResponse.ok("댓글 등록 완료되었습니다", comment, HttpStatus.CREATED);
	}

	@GetMapping("/{lessonId}")
	@Operation(summary = "댓글 조회", description = "레슨 ID에 해당되는 댓글들을 조회합니다.")
	public ResponseEntity<PagedResponse<List<CommentResponseDto>>> readAll(@PathVariable Long lessonId,
		@Valid @ModelAttribute PageRequestDto pageRequestDto) {
		CommentPageResponseDto commentsInfo = commentService.readAll(lessonId, pageRequestDto.getPage(),
			pageRequestDto.getLimit());
		return PagedResponse.ok("댓글 조회 성공", commentsInfo.comments(), commentsInfo.count(), HttpStatus.OK);
	}

	@DeleteMapping("/{commentId}")
	@Operation(summary = "댓글 삭제", description = "댓글 ID에 해당되는 댓글을 삭제합니다.")
	public ResponseEntity<BaseResponse<Void>> deleteComment(@PathVariable Long commentId, @LoginUser Long userId) {
		commentService.delete(commentId, userId);
		return BaseResponse.okOnlyStatus(HttpStatus.NO_CONTENT);
	}
}
