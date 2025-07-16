package com.threestar.trainus.domain.payment.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.coupon.entity.CouponStatus;
import com.threestar.trainus.domain.coupon.entity.UserCoupon;
import com.threestar.trainus.domain.coupon.repository.UserCouponRepository;
import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.lesson.admin.service.AdminLessonService;
import com.threestar.trainus.domain.payment.dto.PaymentRequestDto;
import com.threestar.trainus.domain.payment.dto.PaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.TossPaymentResponseDto;
import com.threestar.trainus.domain.payment.entity.Payment;
import com.threestar.trainus.domain.payment.entity.PaymentMethod;
import com.threestar.trainus.domain.payment.entity.PaymentStatus;
import com.threestar.trainus.domain.payment.entity.TossPayment;
import com.threestar.trainus.domain.payment.repository.PaymentRepository;
import com.threestar.trainus.domain.payment.repository.TossPaymentRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

	private final UserService userService;
	private final AdminLessonService lessonService;
	private final PaymentRepository paymentRepository;
	private final TossPaymentRepository tossPaymentRepository;
	private final UserCouponRepository userCouponRepository;

	@Transactional
	public PaymentResponseDto preparePayment(PaymentRequestDto request, Long userId) {
		Lesson lesson = lessonService.findLessonById(request.getLessonId());
		User user = userService.getUserById(userId);

		int originPrice = lesson.getPrice();

		//쿠폰 적용, 쿠폰 없을수도(안 사용 하는 경우 -> 예외 터트리면 안됨(null인 경우 쿠폰 사용안한 경우) 쿠폰 사용했다면 그에 대해서도 처리 필요)
		int discount = 0;
		UserCoupon coupon = null;
		if (request.getUserCouponId() != null) {
			coupon = userCouponRepository.findById(request.getUserCouponId())
				.filter(c -> c.getStatus() == CouponStatus.ACTIVE)
				.orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
			// discount =
			String discountPrice = coupon.getCoupon().getDiscountPrice();
			if (discountPrice.contains("%")) {
				discount = Integer.parseInt(discountPrice.substring(0, discountPrice.indexOf("%")));
			} else {
				discount = Integer.parseInt(discountPrice);
			}
		}

		int finalPrice = Math.max(0, originPrice - discount);

		String orderId = UUID.randomUUID().toString();

		Payment payment = Payment.builder()
			.user(user)
			.lesson(lesson)
			.orderId(orderId)
			.payPrice(finalPrice)
			.originPrice(originPrice)
			.payDate(LocalDateTime.now())
			.userCoupon(coupon)
			.status(PaymentStatus.READY)
			.paymentMethod(PaymentMethod.TOSS_PAYMENT)
			.build();

		paymentRepository.save(payment);

		return PaymentResponseDto.builder()
			.originPrice(originPrice)
			.lessonTitle(lesson.getLessonName())
			.paymentMethod(PaymentMethod.TOSS_PAYMENT)
			.payPrice(finalPrice)
			.orderId(orderId)
			.build();

	}

	@Transactional
	public void processConfirm(TossPaymentResponseDto tossResponseDto) {
		Payment payment = paymentRepository.findByOrderId(tossResponseDto.getOrderId())
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PAYMENT));

		payment.setPayPrice(tossResponseDto.getTotalAmount());
		payment.setPayDate(LocalDateTime.parse(tossResponseDto.getApprovedAt()));
		payment.setStatus(PaymentStatus.DONE);
		payment.setPaymentMethod(PaymentMethod.valueOf(tossResponseDto.getMethod().toUpperCase()));

		// paymentRepository.save(payment);

		TossPayment tossPayment = TossPayment.builder()
			.payment(payment)
			.paymentKey(tossResponseDto.getPaymentKey())
			.orderId(tossResponseDto.getOrderId())
			.amount(tossResponseDto.getTotalAmount())
			.orderName(tossResponseDto.getOrderName())
			.paymentStatus(PaymentStatus.DONE)
			.paymentMethod(PaymentMethod.valueOf(tossResponseDto.getMethod().toUpperCase()))
			.requestedAt(LocalDateTime.parse(tossResponseDto.getRequestedAt()))
			.approvedAt(LocalDateTime.parse(tossResponseDto.getApprovedAt()))
			.build();

		tossPaymentRepository.save(tossPayment);
	}

	@Transactional
	public void processCancel(TossPaymentResponseDto tossResponseDto) {
		TossPayment tossPayment = tossPaymentRepository.findByPaymentKey(tossResponseDto.getPaymentKey())
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PAYMENT));

		tossPayment.changeStatus(LocalDateTime.parse(tossResponseDto.getRequestedAt()), null, PaymentStatus.CANCELED);

		tossPaymentRepository.save(tossPayment);

		Payment payment = tossPayment.getPayment();
		payment.setStatus(PaymentStatus.CANCELED);
		payment.setCancelledAt(LocalDateTime.now());
		// paymentRepository.save(payment);
	}
}
