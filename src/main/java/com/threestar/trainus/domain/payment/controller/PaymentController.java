package com.threestar.trainus.domain.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.payment.dto.CancelPaymentRequest;
import com.threestar.trainus.domain.payment.dto.ConfirmPaymentRequest;
import com.threestar.trainus.domain.payment.dto.PaymentClient;
import com.threestar.trainus.domain.payment.dto.PaymentRequestDto;
import com.threestar.trainus.domain.payment.dto.PaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.SaveAmountRequest;
import com.threestar.trainus.domain.payment.dto.TossPaymentResponseDto;
import com.threestar.trainus.domain.payment.service.PaymentService;
import com.threestar.trainus.global.unit.BaseResponse;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentClient tossPaymentClient;
	private final PaymentService paymentService;

	@PostMapping("/prepare/{userId}")
	public ResponseEntity<BaseResponse<PaymentResponseDto>> preparePayment(HttpSession session,
		@RequestBody PaymentRequestDto request, @PathVariable("userId") Long userId) {
		// Long userId = (Long)session.getAttribute("LOGIN_USER");
		PaymentResponseDto response = paymentService.preparePayment(request, userId);
		//여기에서 쿠폰 가격 정해서 답변 내보내고 이를 통해 아래 호출
		return BaseResponse.ok("결제 정보 준비 완료", response, HttpStatus.OK);
	}

	@PostMapping("/saveAmount")
	public ResponseEntity<BaseResponse<Void>> saveAmount(HttpSession session, @RequestBody SaveAmountRequest request) {
		session.setAttribute(request.getOrderId(), request.getAmount());
		return BaseResponse.ok("Payment temp save Successful", null, HttpStatus.OK);
	}

	@PostMapping("/verifyAmount")
	public ResponseEntity<BaseResponse<Void>> verifyAmount(HttpSession session,
		@RequestBody SaveAmountRequest request) {
		Integer amount = (Integer)session.getAttribute(request.getOrderId());
		if (amount == null || !amount.equals(request.getAmount())) {
			//todo 세션 삭제 안했는데 괜찮은지
			return BaseResponse.error("결제 금액 정보가 유효하지 않습니다", null, HttpStatus.BAD_REQUEST);
		}

		session.removeAttribute(request.getOrderId());
		return BaseResponse.ok("Payment is valid", null, HttpStatus.OK);
	}

	@PostMapping("/confirm")
	public ResponseEntity<BaseResponse<Void>> confirm(@RequestBody ConfirmPaymentRequest request) {
		TossPaymentResponseDto tossResponse = tossPaymentClient.confirmPayment(request);
		paymentService.processConfirm(tossResponse);
		return BaseResponse.ok("결제 성공", null, HttpStatus.OK);
	}

	@PostMapping("/cancel")
	public ResponseEntity<BaseResponse<Void>> cancel(@RequestBody CancelPaymentRequest request) {
		TossPaymentResponseDto tossResponse = tossPaymentClient.cancelPayment(request);
		paymentService.processCancel(tossResponse);
		return BaseResponse.ok("결제 취소 성공", null, HttpStatus.OK);
	}

	// @PostMapping("/saveAmount")
	// public ResponseEntity<BaseResponse<Void>> tempSave(HttpSession session,
	// 	@RequestBody SaveAmountRequest request) {
	// 	session.setAttribute(request.getOrderId(), request.getAmount());
	// 	return BaseResponse.ok("Payment temp save Successful", null, HttpStatus.OK);
	// }
	//
	// @PostMapping("/verifyAmount")
	// public ResponseEntity<BaseResponse<Void>> verifyAmount(HttpSession session,
	// 	@RequestBody SaveAmountRequest request) {
	// 	String amount = (String)session.getAttribute(request.getOrderId());
	// 	if (amount == null || !amount.equals(request.getAmount())) {
	// 		return BaseResponse.error("결제 금액 정보가 유효하지 않습니다", null, HttpStatus.BAD_REQUEST);
	// 	}
	//
	// 	session.removeAttribute(request.getOrderId());
	//
	// 	return BaseResponse.ok("Payment is valid", null, HttpStatus.OK);
	// }
	//
	// @PostMapping("/confirm")
	// public ResponseEntity<BaseResponse<>> confirm(@RequestBody ConfirmPaymentRequest confirmPaymentRequest) throws
	// 	IOException,
	// 	InterruptedException {
	//
	// 	HttpResponse response = tossPaymentClient.requestConfirm(confirmPaymentRequest);
	// 	if (response.statusCode() == HttpStatus.OK.value()) {
	// 		try {
	//
	// 		} catch ()
	// 	}
	// }
	//
	// @GetMapping("/{id}")
	// public ResponseEntity<BaseResponse<ConfirmPaymentResponse>> getPayment(@PathVariable("id") String backendOrderId) {
	// 	ConfirmPaymentResponse payment = tossPaymentService.getPayment(backendOrderId);
	// 	return BaseResponse.ok("결제 조회 완료", payment, HttpStatus.OK);
	// }
	//
	// @PostMapping("/cancel")
	// public ResponseEntity<BaseResponse<?>> cancelPayment(@RequestBody CancelPaymentRequest cancelPaymentRequest) throws
	// 	IOException,
	// 	InterruptedException {
	// 	HttpResponse response = tossPaymentClient.requestPaymentCancel(cancelPaymentRequest.getPaymentKey(),
	// 		cancelPaymentRequest.getCancelReason());
	// 	if (response.statusCode() == 200)
	// 		tossPaymentService.changePaymentStatus(cancelPaymentRequest.getPaymentKey(),
	// 			PaymentStatus.CANCELED);
	// 	return BaseResponse
	// }

}
