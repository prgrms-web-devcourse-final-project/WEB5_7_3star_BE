package com.threestar.trainus.domain.review.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.review.dto.ReviewCreateRequestDto;
import com.threestar.trainus.domain.review.dto.ReviewCreateResponseDto;
import com.threestar.trainus.domain.review.dto.ReviewPageResponseDto;
import com.threestar.trainus.domain.review.service.ReviewService;
import com.threestar.trainus.global.unit.BaseResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Tag(name = "리뷰 API", description = "리뷰 작성, 조회 관련 API")
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

	private final ReviewService reviewService;

	@PostMapping("/{lessonId}")
	public ResponseEntity<BaseResponse<ReviewCreateResponseDto>> createReview(@PathVariable Long lessonId,
		@RequestBody ReviewCreateRequestDto request,
		HttpSession session) {
		Long userId = (Long)session.getAttribute("LOGIN_USER");
		ReviewCreateResponseDto review = reviewService.createReview(request, lessonId, userId);
		return BaseResponse.ok("작성이 완료됐습니다.", review, HttpStatus.CREATED);
	}

	@GetMapping("/{userId}")
	public ResponseEntity<BaseResponse<ReviewPageResponseDto>> readAll(@PathVariable Long userId,
		@RequestParam("page") int page,
		@RequestParam("pageSize") int pageSize
	) {
		ReviewPageResponseDto reviews = reviewService.readAll(userId, page, pageSize);
		return BaseResponse.ok("조회가 완료됐습니다.", reviews, HttpStatus.OK);
	}

}
