package com.threestar.trainus.domain.lesson.teacher.dto;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.threestar.trainus.domain.lesson.teacher.entity.Category;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class LessonUpdateRequestDtoTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	@DisplayName("레슨명, 설명, 이미지가 있으면 기본 정보 변경으로 판단한다")
	void hasBasicInfoChanges_WhenBasicFieldsPresent() {
		LessonUpdateRequestDto request = new LessonUpdateRequestDto(
			"수정 레슨",
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			List.of("https://cdn.test/1.png")
		);

		assertThat(request.hasBasicInfoChanges()).isTrue();
		assertThat(request.hasRestrictedChanges()).isFalse();
		assertThat(request.hasTimeChanges()).isFalse();
	}

	@Test
	@DisplayName("카테고리, 가격, 위치 등은 제한 필드 변경으로 판단한다")
	void hasRestrictedChanges_WhenRestrictedFieldsPresent() {
		LessonUpdateRequestDto request = new LessonUpdateRequestDto(
			null,
			null,
			Category.PILATES,
			40000,
			null,
			null,
			null,
			null,
			null,
			"서울시",
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null
		);

		assertThat(request.hasBasicInfoChanges()).isFalse();
		assertThat(request.hasRestrictedChanges()).isTrue();
		assertThat(request.hasTimeChanges()).isFalse();
	}

	@Test
	@DisplayName("시작 또는 종료 시간이 있으면 시간 변경으로 판단한다")
	void hasTimeChanges_WhenTimeFieldsPresent() {
		LocalDateTime startAt = LocalDateTime.now().plusDays(2);
		LessonUpdateRequestDto request = new LessonUpdateRequestDto(
			null,
			null,
			null,
			null,
			null,
			startAt,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null
		);

		assertThat(request.hasRestrictedChanges()).isTrue();
		assertThat(request.hasTimeChanges()).isTrue();
	}

	@Test
	@DisplayName("빈 이미지 목록만 있으면 기본 정보 변경으로 판단하지 않는다")
	void hasBasicInfoChanges_WhenOnlyEmptyImages() {
		LessonUpdateRequestDto request = new LessonUpdateRequestDto(
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			List.of()
		);

		assertThat(request.hasBasicInfoChanges()).isFalse();
		assertThat(request.hasRestrictedChanges()).isFalse();
		assertThat(request.hasTimeChanges()).isFalse();
	}

	@Test
	@DisplayName("수정 요청의 숫자/길이 제약을 검증한다")
	void invalidValidationFields_HasViolations() {
		LessonUpdateRequestDto request = new LessonUpdateRequestDto(
			"가".repeat(51),
			"설".repeat(256),
			null,
			-1,
			0,
			null,
			null,
			null,
			null,
				"서울특별시".repeat(3),
			null,
			null,
			null,
			null,
			"상세주소".repeat(10),
			null,
			null,
			List.of("1", "2", "3", "4", "5", "6")
		);

		Set<ConstraintViolation<LessonUpdateRequestDto>> violations = validator.validate(request);

		assertThat(violations)
			.extracting(violation -> violation.getPropertyPath().toString())
			.contains("lessonName", "description", "price", "maxParticipants", "city", "addressDetail", "lessonImages");
	}
}
