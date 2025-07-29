package com.threestar.trainus.domain.payment.mapper;

import java.time.LocalDateTime;
import java.util.List;

import com.threestar.trainus.domain.coupon.user.entity.UserCoupon;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.payment.dto.PaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.TossPaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.cancel.CancelPaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.cancel.PaymentCancelHistoryPageDto;
import com.threestar.trainus.domain.payment.dto.cancel.PaymentCancelHistoryResponseDto;
import com.threestar.trainus.domain.payment.dto.cancel.PaymentCancelPageWrapperDto;
import com.threestar.trainus.domain.payment.dto.success.PaymentSuccessHistoryPageDto;
import com.threestar.trainus.domain.payment.dto.success.PaymentSuccessHistoryResponseDto;
import com.threestar.trainus.domain.payment.dto.success.PaymentSuccessPageWrapperDto;
import com.threestar.trainus.domain.payment.dto.success.SuccessfulPaymentResponseDto;
import com.threestar.trainus.domain.payment.entity.Payment;
import com.threestar.trainus.domain.payment.entity.PaymentMethod;
import com.threestar.trainus.domain.payment.entity.PaymentStatus;
import com.threestar.trainus.domain.payment.entity.TossPayment;
import com.threestar.trainus.domain.user.entity.User;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentMapper {

	public static Payment toPayment(User user, Lesson lesson, String orderId, int finalPrice, UserCoupon coupon,
		PaymentStatus status, PaymentMethod payMethod) {
		return Payment.builder()
			.user(user)
			.lesson(lesson)
			.orderId(orderId)
			.payPrice(finalPrice)
			.originPrice(lesson.getPrice())
			.payDate(LocalDateTime.now())
			.userCoupon(coupon)
			.status(status)
			.paymentMethod(payMethod)
			.build();
	}

	public static TossPayment toTossPayment(Payment payment, TossPaymentResponseDto tossDto, LocalDateTime requestAt,
		LocalDateTime paidAt, PaymentStatus status) {
		return TossPayment.builder()
			.payment(payment)
			.paymentKey(tossDto.paymentKey())
			.orderId(tossDto.orderId())
			.amount(tossDto.totalAmount())
			.orderName(tossDto.orderName())
			.paymentStatus(status)
			.paymentMethod(PaymentMethod.fromTossMethod(tossDto.method()))
			.requestedAt(requestAt)
			.approvedAt(paidAt)
			.build();
	}

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

	public static CancelPaymentResponseDto toFailurePaymentResponseDto(Payment payment, String cancelReason) {
		return CancelPaymentResponseDto.builder()
			.lessonName(payment.getLesson().getLessonName())
			.cancelReason(cancelReason)
			.payPrice(payment.getPayPrice())
			.refundableAmount(payment.getRefundPrice())
			.startAt(payment.getLesson().getStartAt())
			.endAt(payment.getLesson().getEndAt())
			.paymentCancelledAt(payment.getCancelledAt())
			.build();
	}

	public static PaymentSuccessHistoryResponseDto toPaymentSuccessHistoryResponseDto(Payment payment,
		TossPayment tossPayment) {
		return PaymentSuccessHistoryResponseDto.builder()
			.paymentStatus(tossPayment.getPaymentStatus())
			.lessonTitle(payment.getLesson().getLessonName())
			.paymentMethod(payment.getPaymentMethod())
			.payPrice(tossPayment.getAmount())
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

	public static PaymentCancelHistoryResponseDto toPaymentFailureHistoryResponseDto(Payment payment,
		TossPayment tossPayment) {
		return PaymentCancelHistoryResponseDto.builder()
			.paymentStatus(tossPayment.getPaymentStatus())
			.lessonTitle(payment.getLesson().getLessonName())
			.paymentMethod(payment.getPaymentMethod())
			.payPrice(tossPayment.getAmount())
			.refundPrice(payment.getRefundPrice())
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

	public static PaymentCancelHistoryPageDto toPaymentFailureHistoryPageDto(
		List<PaymentCancelHistoryResponseDto> paymentHistory, Integer count) {
		return PaymentCancelHistoryPageDto.builder()
			.failureHistory(paymentHistory)
			.count(count)
			.build();
	}

	public static PaymentCancelPageWrapperDto toPaymentFailurePageWrapperDto(PaymentCancelHistoryPageDto paymentDto) {
		return PaymentCancelPageWrapperDto.builder()
			.failureHistory(paymentDto.failureHistory())
			.build();
	}

	public static PaymentResponseDto toPaymentResponseDto(int originPrice, String lessonTitle, PaymentMethod payMethod ,int finalPrice ,String orderId) {
		return PaymentResponseDto.builder()
			.originPrice(originPrice)
			.lessonTitle(lessonTitle)
			.paymentMethod(payMethod)
			.payPrice(finalPrice)
			.orderId(orderId)
			.build();
	}
}
