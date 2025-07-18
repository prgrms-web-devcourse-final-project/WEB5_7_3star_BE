package com.threestar.trainus.domain.payment.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.threestar.trainus.domain.payment.dto.CancelPaymentRequestDto;
import com.threestar.trainus.domain.payment.dto.ConfirmPaymentRequestDto;
import com.threestar.trainus.domain.payment.dto.PaymentClient;
import com.threestar.trainus.domain.payment.dto.PaymentRequestDto;
import com.threestar.trainus.domain.payment.dto.PaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.SaveAmountRequestDto;
import com.threestar.trainus.domain.payment.dto.TossPaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.failure.FailurePaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.failure.PaymentFailureHistoryPageDto;
import com.threestar.trainus.domain.payment.dto.failure.PaymentFailurePageWrapperDto;
import com.threestar.trainus.domain.payment.dto.success.PaymentSuccessHistoryPageDto;
import com.threestar.trainus.domain.payment.dto.success.PaymentSuccessPageWrapperDto;
import com.threestar.trainus.domain.payment.dto.success.SuccessfulPaymentResponseDto;
import com.threestar.trainus.domain.payment.mapper.PaymentMapper;
import com.threestar.trainus.domain.payment.service.PaymentService;
import com.threestar.trainus.global.unit.BaseResponse;
import com.threestar.trainus.global.unit.PagedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "결제 API", description = "결제, 검증, 취소 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentClient tossPaymentClient;
	private final PaymentService paymentService;

	@Value("${spring.page.size.limit}")
	private int pageSizeLimit;

	@PostMapping("/prepare")
	@Operation(summary = "결제 준비", description = "실제 결제 전 최종 가격 적용 후 결제 준비")
	public ResponseEntity<BaseResponse<PaymentResponseDto>> preparePayment(HttpSession session,
		@RequestBody PaymentRequestDto request) {
		Long userId = (Long)session.getAttribute("LOGIN_USER");
		PaymentResponseDto response = paymentService.preparePayment(request, userId);
		return BaseResponse.ok("결제 정보 준비 완료", response, HttpStatus.OK);
	}

	@PostMapping("/saveAmount")
	@Operation(summary = "결제 검증 데이터 저장", description = "결제 무결성 검증을 위한 데이터 저장")
	public ResponseEntity<BaseResponse<Void>> saveAmount(HttpSession session,
		@RequestBody SaveAmountRequestDto request) {
		session.setAttribute(request.orderId(), request.amount());
		return BaseResponse.ok("Payment temp save Successful", null, HttpStatus.OK);
	}

	@PostMapping("/verifyAmount")
	@Operation(summary = "결제 검증 데이터 확인", description = "결제 무결성 검증을 위한 데이터 확인")
	public ResponseEntity<BaseResponse<Void>> verifyAmount(HttpSession session,
		@RequestBody SaveAmountRequestDto request) {
		Integer amount = (Integer)session.getAttribute(request.orderId());
		try {
			if (amount == null || !amount.equals(request.amount())) {
				return BaseResponse.error("결제 금액 정보가 유효하지 않습니다", null, HttpStatus.BAD_REQUEST);
			}
			return BaseResponse.ok("Payment is valid", null, HttpStatus.OK);
		} finally {
			session.removeAttribute(request.orderId());
		}
	}

	@PostMapping("/confirm")
	@Operation(summary = "결제 진행", description = "결제 진행")
	public ResponseEntity<BaseResponse<SuccessfulPaymentResponseDto>> confirm(
		@RequestBody ConfirmPaymentRequestDto request) {
		TossPaymentResponseDto tossResponse = tossPaymentClient.confirmPayment(request);
		SuccessfulPaymentResponseDto payResult = paymentService.processConfirm(tossResponse);
		return BaseResponse.ok("결제 성공", payResult, HttpStatus.OK);
	}

	@PostMapping("/cancel")
	@Operation(summary = "결제 취소", description = "결제 취소")
	public ResponseEntity<BaseResponse<FailurePaymentResponseDto>> cancel(
		@RequestBody CancelPaymentRequestDto request) {
		TossPaymentResponseDto tossResponse = tossPaymentClient.cancelPayment(request);
		FailurePaymentResponseDto payResult = paymentService.processCancel(tossResponse, request.cancelReason());
		return BaseResponse.ok("결제 취소 성공", payResult, HttpStatus.OK);
	}

	@GetMapping("/view/success")
	@Operation(summary = "완료 결제 조회", description = "완료된 결제 내역 조회")
	public ResponseEntity<PagedResponse<PaymentSuccessPageWrapperDto>> readAll(HttpSession session,
		@RequestParam("page") int page,
		@RequestParam("pageSize") int pageSize) {
		Long userId = (Long)session.getAttribute("LOGIN_USER");
		int correctPage = Math.max(page, 1);
		int correctPageSize = Math.max(1, Math.min(pageSize, pageSizeLimit));
		PaymentSuccessHistoryPageDto paymentSuccessHistoryPageDto = paymentService.viewAllSuccessTransaction(userId,
			correctPage, correctPageSize);
		PaymentSuccessPageWrapperDto payments = PaymentMapper.toPaymentSuccessPageWrapperDto(
			paymentSuccessHistoryPageDto);
		return PagedResponse.ok("성공 결제 조회 성공", payments, paymentSuccessHistoryPageDto.count(), HttpStatus.OK);
	}

	@GetMapping("/view/cancel")
	@Operation(summary = "취소 결제 조회", description = "취소된 결제 내역 조회")
	public ResponseEntity<PagedResponse<PaymentFailurePageWrapperDto>> readAllFailure(HttpSession session,
		@RequestParam("page") int page,
		@RequestParam("pageSize") int pageSize) {
		Long userId = (Long)session.getAttribute("LOGIN_USER");
		int correctPage = Math.max(page, 1);
		int correctPageSize = Math.max(1, Math.min(pageSize, pageSizeLimit));
		PaymentFailureHistoryPageDto paymentFailureHistoryPageDto = paymentService.viewAllFailureTransaction(userId,
			correctPage, correctPageSize);
		PaymentFailurePageWrapperDto payments = PaymentMapper.toPaymentFailurePageWrapperDto(
			paymentFailureHistoryPageDto);
		return PagedResponse.ok("취소 결제 조회 성공", payments, paymentFailureHistoryPageDto.count(), HttpStatus.OK);
	}

	@GetMapping("/{paymentKey}")
	@Operation(summary = "상세 결제 조회", description = "상세 결제 내역 조회")
	public ResponseEntity<BaseResponse<TossPaymentResponseDto>> readDetailPayment(
		@PathVariable("paymentKey") String paymentKey) {
		return BaseResponse.ok("상세 결제 조회 완료", tossPaymentClient.viewDetailPayment(paymentKey), HttpStatus.OK);
	}

}
