package com.threestar.trainus.global.dto;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class PageRequestDtoTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	@DisplayName("기본 page와 limit 값은 validation 위반이 없다")
	void defaultValues_NoViolations() {
		PageRequestDto request = new PageRequestDto();

		assertThat(request.getPage()).isEqualTo(1);
		assertThat(request.getLimit()).isEqualTo(5);
		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	@DisplayName("page와 limit 하한을 벗어나면 validation 위반이 발생한다")
	void belowMinimum_HasViolations() {
		PageRequestDto request = new PageRequestDto();
		request.setPage(0);
		request.setLimit(0);

		assertThat(validator.validate(request))
			.extracting(violation -> violation.getPropertyPath().toString())
			.contains("page", "limit");
	}

	@Test
	@DisplayName("page와 limit 상한을 벗어나면 validation 위반이 발생한다")
	void aboveMaximum_HasViolations() {
		PageRequestDto request = new PageRequestDto();
		request.setPage(1001);
		request.setLimit(101);

		assertThat(validator.validate(request))
			.extracting(violation -> violation.getPropertyPath().toString())
			.contains("page", "limit");
	}
}
