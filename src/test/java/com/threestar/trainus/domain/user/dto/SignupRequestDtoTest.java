package com.threestar.trainus.domain.user.dto;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class SignupRequestDtoTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	@DisplayName("정상 회원가입 요청은 유효하다")
	void validRequest_NoViolations() {
		assertThat(validator.validate(new SignupRequestDto("test@test.com", "password123", "테스트유저"))).isEmpty();
	}

	@Test
	@DisplayName("이메일, 비밀번호, 닉네임 제약을 검증한다")
	void invalidRequest_HasViolations() {
		assertThat(validator.validate(new SignupRequestDto("bad-email", "short", "a")))
			.extracting(violation -> violation.getPropertyPath().toString())
			.contains("email", "password", "nickname");
	}
}
