package com.threestar.trainus.domain.user.dto;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class PasswordUpdateDtoTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	@DisplayName("정상 비밀번호 변경 요청은 유효하다")
	void validRequest_NoViolations() {
		assertThat(validator.validate(new PasswordUpdateDto("current", "newPassword1", "newPassword1"))).isEmpty();
	}

	@Test
	@DisplayName("현재 비밀번호, 새 비밀번호, 확인 비밀번호 제약을 검증한다")
	void invalidRequest_HasViolations() {
		assertThat(validator.validate(new PasswordUpdateDto("", "short", "")))
			.extracting(violation -> violation.getPropertyPath().toString())
			.contains("currentPassword", "newPassword", "confirmPassword");
	}
}
