package com.threestar.trainus.domain.payment.mapper;

import java.time.LocalDateTime;
import java.util.List;

import com.threestar.trainus.domain.payment.dto.failure.FailurePaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.failure.PaymentFailureHistoryPageDto;
import com.threestar.trainus.domain.payment.dto.failure.PaymentFailureHistoryResponseDto;
import com.threestar.trainus.domain.payment.dto.failure.PaymentFailurePageWrapperDto;
import com.threestar.trainus.domain.payment.dto.success.PaymentSuccessHistoryPageDto;
import com.threestar.trainus.domain.payment.dto.success.PaymentSuccessHistoryResponseDto;
import com.threestar.trainus.domain.payment.dto.success.PaymentSuccessPageWrapperDto;
import com.threestar.trainus.domain.payment.dto.success.SuccessfulPaymentResponseDto;
import com.threestar.trainus.domain.payment.entity.Payment;
import com.threestar.trainus.domain.payment.entity.TossPayment;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentMapper {

	public static SuccessfulPaymentResponseDto toSuccessfulPaymentResponseDto(Payment payment) {
		return SuccessfulPaymentResponseDto.builder()
			.payPrice(payment.getPayPrice())
			.paymentMethod(payment.getPaymentMethod())
			.city(payment.getLesson().getCity())
			.district(payment.getLesson().getDistrict())
			.dong(payment.getLesson().getDong())
			.addressDetail(payment.getLesson().getAddressDetail())
			.endAt(payment.getLesson().getEndAt())
			.startAt(payment.getLesson().getStartAt())
			.build();
	}

	public static FailurePaymentResponseDto toFailurePaymentResponseDto(Payment payment, String cancelReason) {
		return FailurePaymentResponseDto.builder()
			.lessonName(payment.getLesson().getLessonName())
			.cancelReason(cancelReason)
			.payPrice(payment.getPayPrice())
			.startAt(payment.getLesson().getStartAt())
			.endAt(payment.getLesson().getEndAt())
			.paymentCancelledAt(payment.getCancelledAt())
			.build();
	}

	public static PaymentSuccessHistoryResponseDto toPaymentSuccessHistoryResponseDto(Payment payment,
		TossPayment tossPayment) {
		return PaymentSuccessHistoryResponseDto.builder()
			.paymentKey(tossPayment.getPaymentKey())
			.paymentStatus(tossPayment.getPaymentStatus())
			.lessonTitle(payment.getLesson().getLessonName())
			.paymentMethod(payment.getPaymentMethod())
			.originalPrice(payment.getPayPrice())
			.dong(payment.getLesson().getDong())
			.district(payment.getLesson().getDistrict())
			.city(payment.getLesson().getCity())
			.detailAddress(payment.getLesson().getAddressDetail())
			.orderId(payment.getOrderId())
			.paymentApprovedAt(tossPayment.getApprovedAt())
			.lessonStartAt(payment.getLesson().getStartAt())
			.lessonEndAt(payment.getLesson().getEndAt())
			.build();
	}

	public static PaymentSuccessHistoryPageDto toPaymentSuccessHistoryPageDto(
		List<PaymentSuccessHistoryResponseDto> paymentHistory, Integer count) {
		return PaymentSuccessHistoryPageDto.builder()
			.successHistory(paymentHistory)
			.count(count)
			.build();
	}

	public static PaymentSuccessPageWrapperDto toPaymentSuccessPageWrapperDto(PaymentSuccessHistoryPageDto paymentDto) {
		return PaymentSuccessPageWrapperDto.builder()
			.successHistory(paymentDto.successHistory())
			.build();
	}

	public static PaymentFailureHistoryResponseDto toPaymentFailureHistoryResponseDto(Payment payment,
		TossPayment tossPayment) {
		return PaymentFailureHistoryResponseDto.builder()
			.paymentKey(tossPayment.getPaymentKey())
			.paymentStatus(tossPayment.getPaymentStatus())
			.lessonTitle(payment.getLesson().getLessonName())
			.paymentMethod(payment.getPaymentMethod())
			.originalPrice(payment.getPayPrice())
			.dong(payment.getLesson().getDong())
			.district(payment.getLesson().getDistrict())
			.city(payment.getLesson().getCity())
			.detailAddress(payment.getLesson().getAddressDetail())
			.orderId(payment.getOrderId())
			.paymentCancelledAt(tossPayment.getApprovedAt())
			.lessonStartAt(payment.getLesson().getStartAt())
			.lessonEndAt(payment.getLesson().getEndAt())
			.cancelReason(tossPayment.getCancelReason())
			.paymentCancelledAt(payment.getCancelledAt())
			.build();
	}

	public static PaymentFailureHistoryPageDto toPaymentFailureHistoryPageDto(
		List<PaymentFailureHistoryResponseDto> paymentHistory, Integer count) {
		return PaymentFailureHistoryPageDto.builder()
			.failureHistory(paymentHistory)
			.count(count)
			.build();
	}

	public static PaymentFailurePageWrapperDto toPaymentFailurePageWrapperDto(PaymentFailureHistoryPageDto paymentDto) {
		return PaymentFailurePageWrapperDto.builder()
			.failureHistory(paymentDto.failureHistory())
			.build();
	}
}
