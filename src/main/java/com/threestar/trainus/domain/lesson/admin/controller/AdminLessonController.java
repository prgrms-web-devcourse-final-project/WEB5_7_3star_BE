package com.threestar.trainus.domain.lesson.admin.controller;

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

import com.threestar.trainus.domain.lesson.admin.dto.ApplicationActionRequestDto;
import com.threestar.trainus.domain.lesson.admin.dto.ApplicationProcessResponseDto;
import com.threestar.trainus.domain.lesson.admin.dto.CreatedLessonListResponseDto;
import com.threestar.trainus.domain.lesson.admin.dto.LessonApplicationListResponseDto;
import com.threestar.trainus.domain.lesson.admin.dto.LessonCreateRequestDto;
import com.threestar.trainus.domain.lesson.admin.dto.LessonResponseDto;
import com.threestar.trainus.domain.lesson.admin.dto.ParticipantListResponseDto;
import com.threestar.trainus.domain.lesson.admin.service.AdminLessonService;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;
import com.threestar.trainus.global.unit.BaseResponse;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * 이 컨트롤러는 전부 강사용 api!!!
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdminLessonController {

	private final AdminLessonService adminLessonService;

	//레슨 생성
	@PostMapping("/lessons")
	public ResponseEntity<BaseResponse<LessonResponseDto>> createLesson(
		@Valid @RequestBody LessonCreateRequestDto requestDto,
		HttpSession session) {

		//로그인한 사용자만 레슨을 생성할 수 있음
		Long sessionUserId = (Long)session.getAttribute("LOGIN_USER");
		if (sessionUserId == null) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
		}

		LessonResponseDto responseDto = adminLessonService.createLesson(requestDto, sessionUserId);
		return BaseResponse.ok("레슨이 생성되었습니다.", responseDto, HttpStatus.CREATED);
	}

	//레슨 삭제
	@DeleteMapping("/lessons/{lessonId}")
	public ResponseEntity<BaseResponse<Void>> deleteLesson(
		@PathVariable Long lessonId,
		HttpSession session) {

		//세션을 기반으로 인증
		Long sessionUserId = (Long)session.getAttribute("LOGIN_USER");
		if (sessionUserId == null) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
		}

		// 레슨 삭제
		adminLessonService.deleteLesson(lessonId, sessionUserId);
		return BaseResponse.okOnlyStatus(HttpStatus.NO_CONTENT);
	}

	//레슨 신청자 목록 조회
	@GetMapping("/lessons/{lessonId}/applications")
	public ResponseEntity<BaseResponse<LessonApplicationListResponseDto>> getLessonApplications(
		@PathVariable Long lessonId,
		@RequestParam(defaultValue = "1") @Min(value = 1, message = "페이지는 1 이상이어야 합니다.") int page,
		@RequestParam(defaultValue = "5") @Min(value = 1, message = "limit는 1 이상이어야 합니다.") int limit,
		@RequestParam(defaultValue = "ALL") String status,
		HttpSession session) {

		// 세션 기반 인증 체크
		Long sessionUserId = (Long)session.getAttribute("LOGIN_USER");
		if (sessionUserId == null) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
		}

		// 신청자 목록 조회
		LessonApplicationListResponseDto responseDto = adminLessonService
			.getLessonApplications(lessonId, page, limit, status, sessionUserId);

		return BaseResponse.ok("레슨 신청자 목록 조회 완료.", responseDto, HttpStatus.OK);
	}

	//레슨 신청 승인/거절
	@PostMapping("/lessons/applications/{lessonApplicationId}")
	public ResponseEntity<BaseResponse<ApplicationProcessResponseDto>> processLessonApplication(
		@PathVariable Long lessonApplicationId,
		@Valid @RequestBody ApplicationActionRequestDto requestDto,
		HttpSession session) {

		// 세션 기반 인증 체크
		Long sessionUserId = (Long)session.getAttribute("LOGIN_USER");
		if (sessionUserId == null) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
		}

		// 신청 승인/거절 처리
		ApplicationProcessResponseDto responseDto = adminLessonService
			.processLessonApplication(lessonApplicationId, requestDto.action(), sessionUserId);

		return BaseResponse.ok("레슨 신청 " + (requestDto.action().equals("APPROVED") ? "승인" : "거절"), responseDto,
			HttpStatus.OK);
	}

	//레슨 참가자 목록 조회
	@GetMapping("/lessons/{lessonId}/participants")
	public ResponseEntity<BaseResponse<ParticipantListResponseDto>> getLessonParticipants(
		@PathVariable Long lessonId,
		@RequestParam(defaultValue = "1") @Min(value = 1, message = "페이지는 1 이상이어야 합니다.") int page,
		@RequestParam(defaultValue = "5") @Min(value = 1, message = "limit는 1 이상이어야 합니다.") int limit,
		HttpSession session) {

		// 세션 기반 인증 체크
		Long sessionUserId = (Long)session.getAttribute("LOGIN_USER");
		if (sessionUserId == null) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
		}

		// 참가자 목록 조회
		ParticipantListResponseDto responseDto = adminLessonService
			.getLessonParticipants(lessonId, page, limit, sessionUserId);

		return BaseResponse.ok("레슨 참가자 목록 조회 완료.", responseDto, HttpStatus.OK);
	}

	//강사가 개설한 레슨 목록 조회
	@GetMapping("/lessons/{userId}/created-lessons")
	public ResponseEntity<BaseResponse<CreatedLessonListResponseDto>> getCreatedLessons(
		@PathVariable Long userId,
		@RequestParam(defaultValue = "1") @Min(value = 1, message = "페이지는 1 이상이어야 합니다.") int page,
		@RequestParam(defaultValue = "5") @Min(value = 1, message = "limit는 1 이상이어야 합니다.") int limit,
		@RequestParam(required = false) String status,
		HttpSession session) {

		// 내가 개설한 레슨만 조회 가능 -> 세션기반인증
		Long sessionUserId = (Long)session.getAttribute("LOGIN_USER");
		if (sessionUserId == null) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
		}

		// 내가 개설한 레슨만 조회 가능!!
		if (!sessionUserId.equals(userId)) {
			throw new BusinessException(ErrorCode.LESSON_ACCESS_FORBIDDEN);
		}

		// 개설한 레슨 목록 조회
		CreatedLessonListResponseDto responseDto = adminLessonService
			.getCreatedLessons(userId, page, limit, status);

		return BaseResponse.ok("개설한 레슨 목록 조회 완료.", responseDto, HttpStatus.OK);
	}

}
