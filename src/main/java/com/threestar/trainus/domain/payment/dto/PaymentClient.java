package com.threestar.trainus.domain.payment.dto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
		log.info("secret-key : {}, url ={} baseUrl = {}, baseUrl2 = {}", paymentProperties.getSecretKey(),
			paymentProperties.getBaseUrl(), paymentProperties.getConfirmEndPoint(),
			paymentProperties.getCancelEndPoint());
		byte[] encodedBytes = Base64.getEncoder()
			.encode((paymentProperties.getSecretKey() + BASIC_DELIMITER).getBytes(StandardCharsets.UTF_8));
		log.info("new Header = {}", AUTH_HEADER_PREFIX + new String(encodedBytes));
		return AUTH_HEADER_PREFIX + new String(encodedBytes);
	}

	public TossPaymentResponseDto confirmPayment(ConfirmPaymentRequest request) {
		log.info("Sending Toss confirm request: orderId={}, amount={}, paymentKey={}",
			request.getOrderId(), request.getAmount(), request.getPaymentKey());

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

	public TossPaymentResponseDto cancelPayment(CancelPaymentRequest request) {
		return restClient.post()
			.uri(String.format(paymentProperties.getCancelEndPoint(), request.getPaymentKey()))
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.retrieve()
			.onStatus(HttpStatusCode::isError, (req, res) -> {
				throw new BusinessException(ErrorCode.CANCEL_PAYMENT_FAILED);
			})
			.body(TossPaymentResponseDto.class);
	}

}
