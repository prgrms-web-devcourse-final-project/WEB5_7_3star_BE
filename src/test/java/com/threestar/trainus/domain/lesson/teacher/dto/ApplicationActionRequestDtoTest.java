package com.threestar.trainus.domain.lesson.teacher.dto;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.threestar.trainus.domain.lesson.teacher.entity.ApplicationAction;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class ApplicationActionRequestDtoTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	@DisplayName("승인/거절 액션이 있으면 validation 위반이 없다")
	void validAction_NoViolations() {
		assertThat(validator.validate(new ApplicationActionRequestDto(ApplicationAction.APPROVED))).isEmpty();
	}

	@Test
	@DisplayName("승인/거절 액션이 없으면 validation 위반이 발생한다")
	void nullAction_HasViolation() {
		assertThat(validator.validate(new ApplicationActionRequestDto(null)))
			.extracting(violation -> violation.getPropertyPath().toString())
			.containsExactly("action");
	}
}
