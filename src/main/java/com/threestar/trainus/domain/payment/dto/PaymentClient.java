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

	// private ClientHttpRequestFactory createPaymentRequestFactory() {
	// 	HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
	// 	factory.setConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS));
	// 	factory.setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS));
	// 	return factory;
	// }

	// public HttpResponse requestConfirm(ConfirmPaymentRequest confirmPaymentRequest) throws
	// 	IOException,
	// 	InterruptedException {
	// 	String tossOrderId = confirmPaymentRequest.getOrderId();
	// 	String amount = confirmPaymentRequest.getAmount();
	// 	String tossPaymentKey = confirmPaymentRequest.getPaymentKey();
	//
	// 	JsonNode requestObj = objectMapper.createObjectNode()
	// 		.put("orderId", tossOrderId)
	// 		.put("amount", amount)
	// 		.put("paymentKey", tossPaymentKey);
	//
	// 	String requestBody = objectMapper.writeValueAsString(requestObj);
	//
	// 	HttpRequest request = HttpRequest.newBuilder()
	// 		.uri(URI.create("<https://api.tosspayments.com/v1/payments/confirm>"))
	// 		.header("Authorization", getAuthorizations())
	// 		.header("Content-Type", "application/json")
	// 		.method("POST", HttpRequest.BodyPublishers.ofString(requestBody))
	// 		.build();
	// 	return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
	// }

	// public HttpResponse requestPaymentCancel(String paymentKey, String cancelReason) throws
	// 	IOException,
	// 	InterruptedException {
	// 	HttpRequest request = HttpRequest.newBuilder()
	// 		.uri(URI.create("<https://api.tosspayments.com/v1/payments/>" + paymentKey + "/cancel"))
	// 		.header("Authorization", getAuthorizations())
	// 		.header("Content-Type", "application/json")
	// 		.method("POST", HttpRequest.BodyPublishers.ofString("{\\cancelReason\\:\\" + cancelReason + "\\}"))
	// 		.build();
	// 	return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
	// }

}
