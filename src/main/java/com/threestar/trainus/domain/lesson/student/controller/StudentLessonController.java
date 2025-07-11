package com.threestar.trainus.domain.lesson.student.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.lesson.student.dto.LessonApplicationResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonDetailResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonSearchListResponseDto;
import com.threestar.trainus.domain.lesson.student.service.StudentLessonService;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;
import com.threestar.trainus.global.unit.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Tag(name = "수강생 레슨 API", description = "수강생 조회/등록 관련 API")
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
public class StudentLessonController {
	private final StudentLessonService studentLessonService;

	@GetMapping("/swagger-test")
	@Operation(summary = "Swagger 체크 api입니다!", description = "API 설명란 입니다!")
	public String swaggerTest(
	) {
		return "Swagger Test Check!";
	}

	@GetMapping("/test-auth")
	public String testAuth(HttpSession session, Principal principal) {
		return "SessionID: " + session.getId() + ", user: " + principal;
	}

	@GetMapping
	@Operation(summary = "레슨 검색 api", description = "category(필수, Default: 전체) / search(선택) / 그외 법정동 선택 필수")
	public ResponseEntity<BaseResponse<LessonSearchListResponseDto>> searchLessons(
		@RequestParam(required = false, defaultValue = "1") int page,
		@RequestParam(required = false, defaultValue = "10") int limit,
		@RequestParam String category,
		@RequestParam(required = false) String search,
		@RequestParam String city,
		@RequestParam String district,
		@RequestParam String dong
	) {
		LessonSearchListResponseDto lessonList = studentLessonService.getLessons(page, limit, category, search, city,
			district, dong);
		return BaseResponse.ok("레슨 검색 조회 완료.", lessonList, HttpStatus.OK);
	}

	@GetMapping("/{lessonId}")
	@Operation(summary = "레슨 상세조회", description = "레슨 ID로 상세 정보를 조회합니다.")
	public ResponseEntity<BaseResponse<LessonDetailResponseDto>> getLessonDetail(
		@PathVariable Long lessonId
	) {
		LessonDetailResponseDto response = studentLessonService.getLessonDetail(lessonId);
		return BaseResponse.ok("레슨 상세 조회 완료", response, HttpStatus.OK);
	}
}