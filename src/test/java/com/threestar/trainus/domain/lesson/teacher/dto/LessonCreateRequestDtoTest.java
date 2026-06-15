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

class LessonCreateRequestDtoTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	@DisplayName("정상 생성 요청은 validation 위반이 없다")
	void validRequest_NoViolations() {
		Set<ConstraintViolation<LessonCreateRequestDto>> violations = validator.validate(validRequest());

		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("필수값이 비어 있으면 validation 위반이 발생한다")
	void blankRequiredFields_HasViolations() {
		LessonCreateRequestDto request = new LessonCreateRequestDto(
			"",
			"",
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			"",
			"",
			"",
			"",
			"",
			"",
			null,
			null,
			List.of()
		);

		assertThat(validator.validate(request))
			.extracting(violation -> violation.getPropertyPath().toString())
			.contains("lessonName", "description", "category", "price", "maxParticipants", "startAt", "endAt",
				"openRun", "city", "district", "dong", "address", "addressDetail", "latitude", "longitude");
	}

	@Test
	@DisplayName("이미지는 최대 5장까지만 허용한다")
	void tooManyImages_HasViolation() {
		LessonCreateRequestDto request = new LessonCreateRequestDto(
			"테스트 레슨",
			"레슨 설명",
			Category.GYM,
			30000,
			10,
			LocalDateTime.now().plusDays(2),
			LocalDateTime.now().plusDays(2).plusHours(2),
			null,
			false,
			"서울시",
			"강남구",
			"역삼동",
			"",
			"테스트 주소",
			"상세 주소",
			37.5665,
			126.9780,
			List.of("1", "2", "3", "4", "5", "6")
		);

		assertThat(validator.validate(request))
			.extracting(violation -> violation.getPropertyPath().toString())
			.contains("lessonImages");
	}

	private LessonCreateRequestDto validRequest() {
		LocalDateTime startAt = LocalDateTime.now().plusDays(2);
		return new LessonCreateRequestDto(
			"테스트 레슨",
			"레슨 설명",
			Category.GYM,
			30000,
			10,
			startAt,
			startAt.plusHours(2),
			null,
			false,
			"서울시",
			"강남구",
			"역삼동",
			"",
			"테스트 주소",
			"상세 주소",
			37.5665,
			126.9780,
			List.of("https://cdn.test/1.png")
		);
	}
}
