package com.threestar.trainus.domain.payment.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;
import com.threestar.trainus.domain.coupon.user.entity.UserCoupon;
import com.threestar.trainus.domain.coupon.user.repository.UserCouponRepository;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.service.AdminLessonService;
import com.threestar.trainus.domain.payment.dto.PaymentRequestDto;
import com.threestar.trainus.domain.payment.dto.PaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.TossPaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.failure.FailurePaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.failure.PaymentFailureHistoryPageDto;
import com.threestar.trainus.domain.payment.dto.failure.PaymentFailureHistoryResponseDto;
import com.threestar.trainus.domain.payment.dto.success.PaymentSuccessHistoryPageDto;
import com.threestar.trainus.domain.payment.dto.success.PaymentSuccessHistoryResponseDto;
import com.threestar.trainus.domain.payment.dto.success.SuccessfulPaymentResponseDto;
import com.threestar.trainus.domain.payment.entity.Payment;
import com.threestar.trainus.domain.payment.entity.PaymentMethod;
import com.threestar.trainus.domain.payment.entity.PaymentStatus;
import com.threestar.trainus.domain.payment.entity.TossPayment;
import com.threestar.trainus.domain.payment.mapper.PaymentMapper;
import com.threestar.trainus.domain.payment.repository.PaymentRepository;
import com.threestar.trainus.domain.payment.repository.TossPaymentRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;
import com.threestar.trainus.global.utils.PageLimitCalculator;

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
		Lesson lesson = lessonService.findLessonById(request.lessonId());
		User user = userService.getUserById(userId);

		int originPrice = lesson.getPrice();
		int discount = 0;

		UserCoupon coupon = null;
		if (request.userCouponId() != null) {
			coupon = userCouponRepository.findById(request.userCouponId())
				.filter(c -> c.getStatus() == CouponStatus.ACTIVE)
				.orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

			Optional<Payment> existing = paymentRepository.findByUserCouponAndStatus(coupon, PaymentStatus.READY);
			if (existing.isPresent()) {
				return PaymentResponseDto.builder()
					.originPrice(existing.get().getOriginPrice())
					.lessonTitle(lesson.getLessonName())
					.paymentMethod(existing.get().getPaymentMethod())
					.payPrice(existing.get().getPayPrice())
					.orderId(existing.get().getOrderId())
					.build();
			}

			String discountPrice = coupon.getCoupon().getDiscountPrice();
			if (discountPrice.contains("%")) {
				int discountPercentage = Integer.parseInt(discountPrice.substring(0, discountPrice.indexOf("%")));
				discount = (originPrice * discountPercentage) / 100;
			} else {
				discount = Integer.parseInt(discountPrice.substring(0, discountPrice.indexOf("원")));
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
			.paymentMethod(PaymentMethod.CREDIT_CARD)
			.build();

		paymentRepository.save(payment);

		return PaymentResponseDto.builder()
			.originPrice(originPrice)
			.lessonTitle(lesson.getLessonName())
			.paymentMethod(PaymentMethod.CREDIT_CARD)
			.payPrice(finalPrice)
			.orderId(orderId)
			.build();
	}

	@Transactional
	public SuccessfulPaymentResponseDto processConfirm(TossPaymentResponseDto tossResponseDto) {
		Payment payment = paymentRepository.findByOrderId(tossResponseDto.orderId())
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PAYMENT));

		if (payment.getStatus() != PaymentStatus.READY) {
			throw new BusinessException(ErrorCode.INVALID_PAYMENT);
		}

		DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
		LocalDateTime paidAt = OffsetDateTime.parse(tossResponseDto.approvedAt(), formatter).toLocalDateTime();
		LocalDateTime requestAt = OffsetDateTime.parse(tossResponseDto.requestedAt(), formatter).toLocalDateTime();

		payment.setPayPrice(tossResponseDto.totalAmount());
		payment.setPayDate(paidAt);
		payment.setStatus(PaymentStatus.DONE);
		payment.setPaymentMethod(PaymentMethod.fromTossMethod(tossResponseDto.method()));

		TossPayment tossPayment = TossPayment.builder()
			.payment(payment)
			.paymentKey(tossResponseDto.paymentKey())
			.orderId(tossResponseDto.orderId())
			.amount(tossResponseDto.totalAmount())
			.orderName(tossResponseDto.orderName())
			.paymentStatus(PaymentStatus.DONE)
			.paymentMethod(PaymentMethod.fromTossMethod(tossResponseDto.method()))
			.requestedAt(requestAt)
			.approvedAt(paidAt)
			.build();

		if (payment.getUserCoupon() != null && payment.getUserCoupon().getStatus() == CouponStatus.ACTIVE) {
			UserCoupon coupon = payment.getUserCoupon();
			coupon.use();
			userCouponRepository.save(coupon);
		}
		tossPaymentRepository.save(tossPayment);

		return PaymentMapper.toSuccessfulPaymentResponseDto(payment);
	}

	@Transactional
	public FailurePaymentResponseDto processCancel(TossPaymentResponseDto tossResponseDto, String cancelReason) {
		TossPayment tossPayment = tossPaymentRepository.findByPaymentKey(tossResponseDto.paymentKey())
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PAYMENT));

		DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
		LocalDateTime requestAt = OffsetDateTime.parse(tossResponseDto.requestedAt(), formatter).toLocalDateTime();

		tossPayment.changeStatus(requestAt, null, PaymentStatus.CANCELED, cancelReason);

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

		return PaymentMapper.toFailurePaymentResponseDto(payment, cancelReason);
	}

	@Transactional(readOnly = true)
	public PaymentSuccessHistoryPageDto viewAllSuccessTransaction(Long userId, int page, int pageSize) {
		List<Payment> allSuccessPayments = paymentRepository.findAllByUserAndStatus(userId, PaymentStatus.DONE.name(),
			(page - 1) * pageSize, pageSize);
		List<PaymentSuccessHistoryResponseDto> dtoList = allSuccessPayments.stream()
			.map(payment -> {
				TossPayment tossPayment = tossPaymentRepository.findByOrderId(payment.getOrderId())
					.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PAYMENT));
				return PaymentMapper.toPaymentSuccessHistoryResponseDto(payment, tossPayment);
			})
			.toList();

		return PaymentMapper.toPaymentSuccessHistoryPageDto(
			dtoList, paymentRepository.count(userId, PaymentStatus.DONE.name(),
				PageLimitCalculator.calculatePageLimit(page, pageSize, 5))
		);
	}

	@Transactional(readOnly = true)
	public PaymentFailureHistoryPageDto viewAllFailureTransaction(Long userId, int page, int pageSize) {
		List<Payment> allFailurePayments = paymentRepository.findAllByUserAndStatus(userId,
			PaymentStatus.CANCELED.name(),
			(page - 1) * pageSize, pageSize);
		List<PaymentFailureHistoryResponseDto> dtoList = allFailurePayments.stream()
			.map(payment -> {
				TossPayment tossPayment = tossPaymentRepository.findByOrderId(payment.getOrderId())
					.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PAYMENT));
				return PaymentMapper.toPaymentFailureHistoryResponseDto(payment, tossPayment);
			})
			.toList();

		return PaymentMapper.toPaymentFailureHistoryPageDto(
			dtoList, paymentRepository.count(userId, PaymentStatus.CANCELED.name(),
				PageLimitCalculator.calculatePageLimit(page, pageSize, 5))
		);
	}
}