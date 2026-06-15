package com.threestar.trainus.domain.user.dto;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class LoginRequestDtoTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	@DisplayName("올바른 이메일과 비밀번호는 유효하다")
	void validRequest_NoViolations() {
		assertThat(validator.validate(new LoginRequestDto("test@test.com", "password123"))).isEmpty();
	}

	@Test
	@DisplayName("이메일 형식과 필수 입력을 검증한다")
	void invalidRequest_HasViolations() {
		assertThat(validator.validate(new LoginRequestDto("not-email", "")))
			.extracting(violation -> violation.getPropertyPath().toString())
			.contains("email", "password");
	}
}
