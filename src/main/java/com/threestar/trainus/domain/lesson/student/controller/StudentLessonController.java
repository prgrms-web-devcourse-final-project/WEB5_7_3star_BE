package com.threestar.trainus.domain.lesson.student.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.lesson.student.dto.LessonApplicationResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonDetailResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonSearchListResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonSearchResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.LessonSimpleResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.MyLessonApplicationListResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.MyLessonApplicationResponseDto;
import com.threestar.trainus.domain.lesson.student.service.StudentLessonService;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;
import com.threestar.trainus.global.unit.BaseResponse;
import com.threestar.trainus.global.unit.PagedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "수강생 레슨 API", description = "수강생 조회/등록 관련 API")
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
public class StudentLessonController {
	private final StudentLessonService studentLessonService;

	@GetMapping
	@Operation(summary = "레슨 검색 api", description = "category(필수, Default: \"ALL\") / search(선택) / 그외 법정동 선택 필수")
	public ResponseEntity<PagedResponse<List<LessonSearchResponseDto>>> searchLessons(
		@RequestParam(defaultValue = "1") @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
		@Max(value = 1000, message = "페이지는 1000 이하여야 합니다.") int page,
		@RequestParam(defaultValue = "5") @Min(value = 1, message = "limit는 1 이상이어야 합니다.")
		@Max(value = 100, message = "limit는 100 이하여야 합니다.") int limit,
		@RequestParam String category,
		@RequestParam(required = false) String search,
		@RequestParam String city,
		@RequestParam String district,
		@RequestParam String dong
	) {
		LessonSearchListResponseDto lessonList = studentLessonService.searchLessons(page, limit, category, search, city,
			district, dong);
		return PagedResponse.ok("레슨 검색 조회 완료.", lessonList.data(), lessonList.count(), HttpStatus.OK);
	}

	@GetMapping("/{lessonId}")
	@Operation(summary = "레슨 상세조회", description = "레슨 ID로 상세 정보를 조회합니다.")
	public ResponseEntity<BaseResponse<LessonDetailResponseDto>> getLessonDetail(
		@PathVariable Long lessonId
	) {
		LessonDetailResponseDto response = studentLessonService.getLessonDetail(lessonId);
		return BaseResponse.ok("레슨 상세 조회 완료", response, HttpStatus.OK);
	}

	@PostMapping("/{lessonId}/application")
	@Operation(summary = "레슨 신청", description = "레슨 ID에 해당되는 레슨을 신청합니다.")
	public ResponseEntity<BaseResponse<LessonApplicationResponseDto>> createLessonApplication(
		@PathVariable Long lessonId,
		HttpSession session
	) {
		Long userId = (Long)session.getAttribute("LOGIN_USER");
		if (userId == null) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
		}
		LessonApplicationResponseDto response = studentLessonService.applyToLesson(lessonId, userId);
		return BaseResponse.ok("레슨 신청 완료", response, HttpStatus.OK);
	}

	@DeleteMapping("/{lessonId}/application")
	@Operation(summary = "레슨 신청 취소", description = "레슨 ID에 해당되는 레슨 신청을 취소합니다.")
	public ResponseEntity<BaseResponse<Void>> deleteLessonApplication(
		@PathVariable Long lessonId,
		HttpSession session
	) {
		Long userId = (Long)session.getAttribute("LOGIN_USER");
		if (userId == null) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
		}
		studentLessonService.cancelLessonApplication(lessonId, userId);
		return BaseResponse.okOnlyStatus(HttpStatus.NO_CONTENT);
	}

	@GetMapping("/my-applications")
	@Operation(summary = "나의 레슨 신청 목록 조회", description = "현재 로그인한 사용자의 레슨 신청 목록을 조회합니다.")
	public ResponseEntity<PagedResponse<List<MyLessonApplicationResponseDto>>> getMyLessonApplications(
		@RequestParam(defaultValue = "1") @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
		@Max(value = 1000, message = "페이지는 1000 이하여야 합니다.") int page,
		@RequestParam(defaultValue = "5") @Min(value = 1, message = "limit는 1 이상이어야 합니다.")
		@Max(value = 100, message = "limit는 100 이하여야 합니다.") int limit,
		@RequestParam(defaultValue = "ALL") String status,
		HttpSession session
	) {
		Long userId = (Long)session.getAttribute("LOGIN_USER");
		if (userId == null) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
		}

		MyLessonApplicationListResponseDto result = studentLessonService.getMyLessonApplications(userId, page, limit,
			status);

		return PagedResponse.ok("나의 레슨 신청 목록 조회 완료.", result.data(), result.count(), HttpStatus.OK);
	}

	@GetMapping("/summary/{lessonId}")
	@Operation(summary = "레슨 간단 조회", description = "레슨 ID로 간단 정보를 조회합니다.")
	public ResponseEntity<BaseResponse<LessonSimpleResponseDto>> getLessonSimple(
		@PathVariable Long lessonId
	) {
		LessonSimpleResponseDto response = studentLessonService.getLessonSimple(lessonId);
		return BaseResponse.ok("레슨 간단 조회 완료.", response, HttpStatus.OK);
	}
}
