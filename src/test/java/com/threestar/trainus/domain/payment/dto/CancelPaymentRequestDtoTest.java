package com.threestar.trainus.domain.payment.dto;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.threestar.trainus.domain.payment.dto.cancel.CancelPaymentRequestDto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class CancelPaymentRequestDtoTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	@DisplayName("주문 ID와 취소 사유가 있으면 유효하다")
	void validRequest_NoViolations() {
		assertThat(validator.validate(new CancelPaymentRequestDto("order-1", "일정 변경"))).isEmpty();
	}

	@Test
	@DisplayName("주문 ID와 취소 사유가 없으면 validation 위반이 발생한다")
	void nullRequiredFields_HasViolations() {
		assertThat(validator.validate(new CancelPaymentRequestDto(null, null)))
			.extracting(violation -> violation.getPropertyPath().toString())
			.contains("orderId", "cancelReason");
	}
}
