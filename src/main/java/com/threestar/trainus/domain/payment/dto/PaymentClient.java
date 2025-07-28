package com.threestar.trainus.domain.payment.dto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.threestar.trainus.domain.payment.dto.cancel.TossCancelRequestDto;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PaymentClient {
	private static final String BASIC_DELIMITER = ":";
	private static final String AUTH_HEADER_PREFIX = "Basic ";

	private final PaymentProperties paymentProperties;
	private final RestClient restClient;

	public PaymentClient(PaymentProperties paymentProperties) {
		this.paymentProperties = paymentProperties;
		this.restClient = RestClient.builder()
			.baseUrl(paymentProperties.getBaseUrl())
			.defaultHeader(HttpHeaders.AUTHORIZATION, createPaymentAuthHeader(paymentProperties))
			.build();
	}

	private String createPaymentAuthHeader(PaymentProperties paymentProperties) {
		byte[] encodedBytes = Base64.getEncoder()
			.encode((paymentProperties.getSecretKey() + BASIC_DELIMITER).getBytes(StandardCharsets.UTF_8));
		return AUTH_HEADER_PREFIX + new String(encodedBytes);
	}

	public TossPaymentResponseDto confirmPayment(ConfirmPaymentRequestDto request) {

		return restClient.post()
			.uri(paymentProperties.getConfirmEndPoint())
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.retrieve()
			.onStatus(HttpStatusCode::isError, (req, res) -> {
				throw new BusinessException(ErrorCode.CONFIRM_PAYMENT_FAILED);
			})
			.body(TossPaymentResponseDto.class);
	}

	public TossPaymentResponseDto cancelPayment(TossCancelRequestDto request) {
		return restClient.post()
			.uri(String.format(paymentProperties.getCancelEndPoint(), request.paymentKey()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.retrieve()
			.onStatus(HttpStatusCode::isError, (req, res) -> {
				throw new BusinessException(ErrorCode.CANCEL_PAYMENT_FAILED);
			})
			.body(TossPaymentResponseDto.class);
	}
}
