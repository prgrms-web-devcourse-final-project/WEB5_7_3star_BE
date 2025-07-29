package com.threestar.trainus.domain.review.controller;

import org.springframework.beans.factory.annotation.Value;
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
import com.threestar.trainus.domain.review.dto.ReviewPageWrapperDto;
import com.threestar.trainus.domain.review.mapper.ReviewMapper;
import com.threestar.trainus.domain.review.service.ReviewService;
import com.threestar.trainus.global.annotation.LoginUser;
import com.threestar.trainus.global.unit.BaseResponse;
import com.threestar.trainus.global.unit.PagedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "리뷰 API", description = "리뷰 작성, 조회 관련 API")
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

	private final ReviewService reviewService;
	@Value("${spring.page.size.limit}")
	private int pageSizeLimit;

	@PostMapping("/{lessonId}")
	@Operation(summary = "리뷰 작성", description = "레슨 ID에 해당되는 리뷰를 작성합니다.")
	public ResponseEntity<BaseResponse<ReviewCreateResponseDto>> createReview(@PathVariable Long lessonId,
		@Valid @RequestBody ReviewCreateRequestDto request,
		@LoginUser Long userId) {
		ReviewCreateResponseDto review = reviewService.createReview(request, lessonId, userId);
		return BaseResponse.ok("작성이 완료됐습니다.", review, HttpStatus.CREATED);
	}

	@GetMapping("/{userId}")
	@Operation(summary = "리뷰 조회", description = "유저 ID에 해당되는 리뷰들을 조회합니다.")
	public ResponseEntity<PagedResponse<ReviewPageWrapperDto>> readAll(@PathVariable Long userId,
		@RequestParam("page") int page,
		@RequestParam("pageSize") int pageSize
	) {
		int correctPage = Math.max(page, 1);
		int correctPageSize = Math.max(1, Math.min(pageSize, pageSizeLimit));
		ReviewPageResponseDto reviewsInfo = reviewService.readAll(userId, correctPage, correctPageSize);
		ReviewPageWrapperDto reviews = ReviewMapper.toReviewPageWrapperDto(reviewsInfo);
		return PagedResponse.ok("조회가 완료됐습니다.", reviews, reviewsInfo.count(), HttpStatus.OK);
	}
	/*
	 * TODO:구조 통일
	 * */
}
