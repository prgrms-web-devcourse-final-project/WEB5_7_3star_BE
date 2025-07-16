package com.threestar.trainus.domain.payment.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;
import com.threestar.trainus.domain.coupon.user.entity.UserCoupon;
import com.threestar.trainus.domain.coupon.user.repository.UserCouponRepository;
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
			coupon.use();
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

		DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
		LocalDateTime paidAt = OffsetDateTime.parse(tossResponseDto.getApprovedAt(), formatter).toLocalDateTime();

		LocalDateTime requestAt = OffsetDateTime.parse(tossResponseDto.getRequestedAt(), formatter).toLocalDateTime();

		payment.setPayDate(paidAt);
		payment.setStatus(PaymentStatus.DONE);
		payment.setPaymentMethod(PaymentMethod.fromTossMethod(tossResponseDto.getMethod()));

		TossPayment tossPayment = TossPayment.builder()
			.payment(payment)
			.paymentKey(tossResponseDto.getPaymentKey())
			.orderId(tossResponseDto.getOrderId())
			.amount(tossResponseDto.getTotalAmount())
			.orderName(tossResponseDto.getOrderName())
			.paymentStatus(PaymentStatus.DONE)
			.paymentMethod(PaymentMethod.fromTossMethod(tossResponseDto.getMethod()))
			.requestedAt(requestAt)
			.approvedAt(paidAt)
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

		//쿠폰 복원
		if (payment.getUserCoupon() != null) {
			UserCoupon coupon = payment.getUserCoupon();
			if (coupon.getStatus() == CouponStatus.INACTIVE) {
				coupon.restore();
				userCouponRepository.save(coupon);
			}
		}
	}
}
