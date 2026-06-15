package com.threestar.trainus.domain.payment.dto;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class PaymentRequestDtoTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	@DisplayName("lessonId가 있으면 쿠폰이 없어도 유효하다")
	void validWithoutCoupon_NoViolations() {
		assertThat(validator.validate(new PaymentRequestDto(1L, null))).isEmpty();
	}

	@Test
	@DisplayName("lessonId가 없으면 validation 위반이 발생한다")
	void nullLessonId_HasViolation() {
		assertThat(validator.validate(new PaymentRequestDto(null, 10L)))
			.extracting(violation -> violation.getPropertyPath().toString())
			.containsExactly("lessonId");
	}
}
