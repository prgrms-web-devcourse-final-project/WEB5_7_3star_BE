package com.threestar.trainus.domain.payment.dto;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class ConfirmPaymentRequestDtoTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	@DisplayName("주문 ID와 paymentKey가 있으면 유효하다")
	void validRequest_NoViolations() {
		assertThat(validator.validate(new ConfirmPaymentRequestDto(10000, "order-1", "payment-key"))).isEmpty();
	}

	@Test
	@DisplayName("주문 ID와 paymentKey가 없으면 validation 위반이 발생한다")
	void nullRequiredFields_HasViolations() {
		assertThat(validator.validate(new ConfirmPaymentRequestDto(10000, null, null)))
			.extracting(violation -> violation.getPropertyPath().toString())
			.contains("orderId", "paymentKey");
	}
}
