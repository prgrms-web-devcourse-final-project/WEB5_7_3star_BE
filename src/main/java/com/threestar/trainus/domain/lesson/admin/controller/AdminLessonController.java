package com.threestar.trainus.domain.lesson.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.lesson.admin.dto.LessonCreateRequestDto;
import com.threestar.trainus.domain.lesson.admin.dto.LessonResponseDto;
import com.threestar.trainus.domain.lesson.admin.service.AdminLessonService;
import com.threestar.trainus.global.unit.BaseResponse;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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
		Long userId = (Long)session.getAttribute("userId");
		if (userId == null) {
			//테스트용
			userId = 1L;
			//throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
		}
		LessonResponseDto responseDto = adminLessonService.createLesson(requestDto, userId);
		return BaseResponse.ok("레슨이 생성되었습니다.", responseDto, HttpStatus.CREATED);
	}

	//레슨 삭제
	@DeleteMapping("/lessons/{lessonId}")
	public ResponseEntity<BaseResponse<Void>> deleteLesson(
		@PathVariable Long lessonId,
		HttpSession session) {

		//세션을 기반으로 인증
		Long userId = (Long)session.getAttribute("userId");
		if (userId == null) {
			userId = 1L; // 테스트용 임시 값
			//TODO: user업뎃하면, throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
		}

		// 레슨 삭제
		adminLessonService.deleteLesson(lessonId, userId);
		return BaseResponse.okOnlyStatus(HttpStatus.NO_CONTENT);
	}

}
