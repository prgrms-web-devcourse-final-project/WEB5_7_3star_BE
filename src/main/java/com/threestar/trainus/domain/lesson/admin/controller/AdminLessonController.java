package com.threestar.trainus.domain.lesson.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.lesson.admin.dto.ApplicationActionRequestDto;
import com.threestar.trainus.domain.lesson.admin.dto.ApplicationProcessResponseDto;
import com.threestar.trainus.domain.lesson.admin.dto.CreatedLessonListResponseDto;
import com.threestar.trainus.domain.lesson.admin.dto.CreatedLessonListWrapperDto;
import com.threestar.trainus.domain.lesson.admin.dto.LessonApplicationListResponseDto;
import com.threestar.trainus.domain.lesson.admin.dto.LessonApplicationListWrapperDto;
import com.threestar.trainus.domain.lesson.admin.dto.LessonCreateRequestDto;
import com.threestar.trainus.domain.lesson.admin.dto.LessonResponseDto;
import com.threestar.trainus.domain.lesson.admin.dto.ParticipantListResponseDto;
import com.threestar.trainus.domain.lesson.admin.dto.ParticipantListWrapperDto;
import com.threestar.trainus.domain.lesson.admin.entity.ApplicationAction;
import com.threestar.trainus.domain.lesson.admin.mapper.CreatedLessonMapper;
import com.threestar.trainus.domain.lesson.admin.mapper.LessonApplicationMapper;
import com.threestar.trainus.domain.lesson.admin.mapper.LessonParticipantMapper;
import com.threestar.trainus.domain.lesson.admin.service.AdminLessonService;
import com.threestar.trainus.global.annotation.LoginUser;
import com.threestar.trainus.global.dto.PageRequestDto;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;
import com.threestar.trainus.global.unit.BaseResponse;
import com.threestar.trainus.global.unit.PagedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 이 컨트롤러는 전부 강사용 api!!!
 */
@Tag(name = "강사용 레슨 API", description = "레슨 개설, 삭제, 승인/거절, 조회(수강생조회,내레슨조회등) 관련 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdminLessonController {

	private final AdminLessonService adminLessonService;

	//레슨 생성
	@PostMapping("/lessons")
	@Operation(summary = "레슨 생성 api", description = "레슨생성")
	public ResponseEntity<BaseResponse<LessonResponseDto>> createLesson(
		@Valid @RequestBody LessonCreateRequestDto requestDto,
		@LoginUser Long loginUserId) {

		LessonResponseDto responseDto = adminLessonService.createLesson(requestDto, loginUserId);
		return BaseResponse.ok("레슨이 생성되었습니다.", responseDto, HttpStatus.CREATED);
	}

	//레슨 삭제
	@DeleteMapping("/lessons/{lessonId}")
	@Operation(summary = "레슨 삭제 api", description = "현재는 무료 레슨만 있기때문에 참가자가 있어도 마음대로 삭제가능")
	public ResponseEntity<BaseResponse<Void>> deleteLesson(
		@PathVariable Long lessonId,
		@LoginUser Long loginUserId) {

		// 레슨 삭제
		adminLessonService.deleteLesson(lessonId, loginUserId);
		return BaseResponse.okOnlyStatus(HttpStatus.NO_CONTENT);
	}

	//레슨 신청자 목록 조회
	@GetMapping("/lessons/{lessonId}/applications")
	@Operation(summary = "레슨 신청자 목록 조회 api", description = "레슨 신청자의 목록을 조회 가능함.")
	public ResponseEntity<PagedResponse<LessonApplicationListWrapperDto>> getLessonApplications(
		@PathVariable Long lessonId,
		@Valid @ModelAttribute PageRequestDto pageRequestDto,
		@RequestParam(defaultValue = "ALL") String status,
		@LoginUser Long loginUserId) {

		// 신청자 목록 조회
		LessonApplicationListResponseDto responseDto = adminLessonService
			.getLessonApplications(lessonId, pageRequestDto.getPage(), pageRequestDto.getLimit(), status, loginUserId);

		LessonApplicationListWrapperDto wrapperDto = LessonApplicationMapper
			.toLessonApplicationListWrapperDto(responseDto);

		return PagedResponse.ok("레슨 신청자 목록 조회 완료.", wrapperDto, responseDto.count(), HttpStatus.OK);
	}

	//레슨 신청 승인/거절
	@PostMapping("/lessons/applications/{lessonApplicationId}")
	@Operation(summary = "레슨 신청 승인/거절 api", description = "")
	public ResponseEntity<BaseResponse<ApplicationProcessResponseDto>> processLessonApplication(
		@PathVariable Long lessonApplicationId,
		@Valid @RequestBody ApplicationActionRequestDto requestDto,
		@LoginUser Long loginUserId) {

		// 신청 승인/거절 처리
		ApplicationProcessResponseDto responseDto = adminLessonService
			.processLessonApplication(lessonApplicationId, requestDto.action(), loginUserId);

		String message = (requestDto.action() == ApplicationAction.APPROVED) ? "승인" : "거절";
		return BaseResponse.ok("레슨 신청 " + message, responseDto, HttpStatus.OK);
	}

	//레슨 참가자 목록 조회
	@GetMapping("/lessons/{lessonId}/participants")
	@Operation(summary = "레슨 참가자 목록 조회 api", description = "")
	public ResponseEntity<PagedResponse<ParticipantListWrapperDto>> getLessonParticipants(
		@PathVariable Long lessonId,
		@Valid @ModelAttribute PageRequestDto pageRequestDto,
		@LoginUser Long loginUserId) {

		// 참가자 목록 조회
		ParticipantListResponseDto responseDto = adminLessonService
			.getLessonParticipants(lessonId, pageRequestDto.getPage(), pageRequestDto.getLimit(), loginUserId);

		ParticipantListWrapperDto wrapperDto = LessonParticipantMapper
			.toParticipantListWrapperDto(responseDto);

		return PagedResponse.ok("레슨 참가자 목록 조회 완료.", wrapperDto, responseDto.count(), HttpStatus.OK);
	}

	//강사가 개설한 레슨 목록 조회
	@GetMapping("/lessons/{userId}/created-lessons")
	@Operation(summary = "강사가 개설한 레슨 목록 조회 api", description = "")
	public ResponseEntity<PagedResponse<CreatedLessonListWrapperDto>> getCreatedLessons(
		@PathVariable Long userId,
		@Valid @ModelAttribute PageRequestDto pageRequestDto,
		@RequestParam(required = false) String status,
		@LoginUser Long loginUserId) {

		// 내가 개설한 레슨만 조회 가능!!
		if (!loginUserId.equals(userId)) {
			throw new BusinessException(ErrorCode.LESSON_ACCESS_FORBIDDEN);
		}

		// 개설한 레슨 목록 조회
		CreatedLessonListResponseDto responseDto = adminLessonService
			.getCreatedLessons(userId, pageRequestDto.getPage(), pageRequestDto.getLimit(), status);

		CreatedLessonListWrapperDto wrapperDto = CreatedLessonMapper
			.toCreatedLessonListWrapperDto(responseDto);

		return PagedResponse.ok("개설한 레슨 목록 조회 완료.", wrapperDto, responseDto.count(), HttpStatus.OK);
	}

}
