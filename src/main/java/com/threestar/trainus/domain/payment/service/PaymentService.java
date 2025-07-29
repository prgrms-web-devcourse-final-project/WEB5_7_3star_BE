package com.threestar.trainus.domain.payment.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.coupon.user.entity.UserCoupon;
import com.threestar.trainus.domain.coupon.user.service.CouponService;
import com.threestar.trainus.domain.lesson.student.service.StudentLessonService;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonParticipant;
import com.threestar.trainus.domain.lesson.teacher.entity.ParticipantStatus;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonParticipantRepository;
import com.threestar.trainus.domain.lesson.teacher.service.AdminLessonService;
import com.threestar.trainus.domain.payment.dto.ConfirmPaymentRequestDto;
import com.threestar.trainus.domain.payment.dto.PaymentClient;
import com.threestar.trainus.domain.payment.dto.PaymentRequestDto;
import com.threestar.trainus.domain.payment.dto.PaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.TossPaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.cancel.CancelPaymentRequestDto;
import com.threestar.trainus.domain.payment.dto.cancel.CancelPaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.cancel.PaymentCancelHistoryPageDto;
import com.threestar.trainus.domain.payment.dto.cancel.PaymentCancelHistoryResponseDto;
import com.threestar.trainus.domain.payment.dto.cancel.TossCancelRequestDto;
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
import com.threestar.trainus.global.utils.RefundPolicyUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

	private final UserService userService;
	private final PaymentClient paymentClient;
	private final AdminLessonService lessonService;
	private final CouponService couponService;
	private final StudentLessonService studentLessonService;
	private final PaymentRepository paymentRepository;
	private final TossPaymentRepository tossPaymentRepository;
	private final LessonParticipantRepository lessonParticipantRepository;

	private final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	@Transactional
	public PaymentResponseDto preparePayment(PaymentRequestDto request, Long userId) {
		Lesson lesson = lessonService.findLessonById(request.lessonId());
		User user = userService.getUserById(userId);

		//올바른 결제자인지 체크
		studentLessonService.checkValidLessonParticipant(lesson, user);

		// 중복 결제 방지
		validateDuplicatedPayment(lesson, user);

		int discount = 0;
		UserCoupon coupon = null;
		if (request.userCouponId() != null) {  //쿠폰이 있는 주문 내역(실제 결제 진행 전) 불러오기
			coupon = couponService.getValidUserCoupon(request.userCouponId(), userId);

			Optional<Payment> existingCoupon = paymentRepository.findByUserAndLessonAndUserCouponAndStatus(user, lesson,
				coupon, PaymentStatus.READY);
			if (existingCoupon.isPresent()) {
				return PaymentMapper.toPaymentResponseDto(existingCoupon.get().getOriginPrice(),
					lesson.getLessonName(),
					existingCoupon.get().getPaymentMethod(),
					existingCoupon.get().getPayPrice(),
					existingCoupon.get().getOrderId());
			}
			discount = couponService.calculateDiscountedPrice(lesson.getPrice(), coupon);
		} else {
			Optional<Payment> existingNoneCoupon = paymentRepository.findByUserAndLessonAndUserCouponIsNullAndStatus(
				user, lesson,
				PaymentStatus.READY);
			if (existingNoneCoupon.isPresent()) {
				return PaymentMapper.toPaymentResponseDto(existingNoneCoupon.get().getOriginPrice(),
					lesson.getLessonName(),
					existingNoneCoupon.get().getPaymentMethod(),
					existingNoneCoupon.get().getPayPrice(),
					existingNoneCoupon.get().getOrderId());
			}
		}

		int finalPrice = Math.max(0, lesson.getPrice() - discount);
		String orderId = UUID.randomUUID().toString();
		Payment payment = PaymentMapper.toPayment(user, lesson, orderId, finalPrice, coupon, PaymentStatus.READY,
			PaymentMethod.CREDIT_CARD);

		paymentRepository.save(payment);

		return PaymentMapper.toPaymentResponseDto(lesson.getPrice(), lesson.getLessonName(), PaymentMethod.CREDIT_CARD,
			finalPrice, orderId);
	}

	@Transactional
	public SuccessfulPaymentResponseDto processConfirm(ConfirmPaymentRequestDto request) {

		TossPaymentResponseDto tossResponseDto = paymentClient.confirmPayment(request);

		Payment payment = paymentRepository.findByOrderId(tossResponseDto.orderId())
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PAYMENT));

		if (payment.getStatus() != PaymentStatus.READY) {
			throw new BusinessException(ErrorCode.INVALID_PAYMENT);
		}

		LocalDateTime paidAt = OffsetDateTime.parse(tossResponseDto.approvedAt(), formatter).toLocalDateTime();
		LocalDateTime requestAt = OffsetDateTime.parse(tossResponseDto.requestedAt(), formatter).toLocalDateTime();

		payment.processPayment(tossResponseDto.totalAmount(), paidAt, tossResponseDto.method());

		LessonParticipant participant = lessonParticipantRepository
			.findByLessonIdAndUserIdAndStatus(
				payment.getLesson().getId(),
				payment.getUser().getId(),
				ParticipantStatus.PAYMENT_PENDING
			)
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LESSON_PARTICIPANT));

		participant.completePayment();
		lessonParticipantRepository.save(participant);

		TossPayment tossPayment = PaymentMapper.toTossPayment(payment, tossResponseDto, requestAt, paidAt,
			PaymentStatus.DONE);

		if (payment.getUserCoupon() != null) {
			couponService.useCoupon(payment.getUserCoupon());
		}
		tossPaymentRepository.save(tossPayment);

		return PaymentMapper.toSuccessfulPaymentResponseDto(payment);
	}

	@Transactional
	public CancelPaymentResponseDto processCancel(CancelPaymentRequestDto request) {
		TossPayment tossPayment = tossPaymentRepository.findByOrderId(request.orderId())
			.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PAYMENT));

		Payment payment = tossPayment.getPayment();
		LocalDateTime cancelTime = LocalDateTime.now();

		//날짜 검증(취소 가능은 24시간 전까지)
		if (cancelTime.isAfter(payment.getLesson().getStartAt().minusDays(1))) {
			throw new BusinessException(ErrorCode.INVALID_CANCEL_DATE);
		}

		int refundPrice = RefundPolicyUtils.getRefundPrice(cancelTime, payment);

		//여기서 client 호출
		TossPaymentResponseDto tossResponse = paymentClient.cancelPayment(
			new TossCancelRequestDto(tossPayment.getPaymentKey(), request.cancelReason(), refundPrice));

		LocalDateTime cancelAt = OffsetDateTime.parse(tossResponse.cancels().getFirst().canceledAt(), formatter)
			.toLocalDateTime();

		tossPayment.changeStatus(cancelAt, PaymentStatus.CANCELED, request.cancelReason());
		tossPaymentRepository.save(tossPayment);

		payment.cancelPayment(cancelAt, refundPrice);

		// lesson 관련 데이터 업데이트(lessonRepository 삭제 및 lesson 데이터 변경)
		studentLessonService.cancelPayment(payment.getLesson().getId(), payment.getUser().getId());

		//쿠폰 복원
		if (payment.getUserCoupon() != null) {
			UserCoupon coupon = payment.getUserCoupon();
			payment.setUserCoupon(null);   //payment에서도 쿠폰 제거
			couponService.restoreCoupon(coupon);
		}
		paymentRepository.save(payment);

		return PaymentMapper.toFailurePaymentResponseDto(payment, tossPayment.getCancelReason());
	}

	@Transactional(readOnly = true)
	public PaymentSuccessHistoryPageDto viewAllSuccessTransaction(Long userId, int page, int pageSize) {
		List<Long> paymentIds = paymentRepository.findPaymentIdsByUserAndStatus(userId,
			PaymentStatus.DONE.name(), (page - 1) * pageSize, pageSize);

		if (paymentIds.isEmpty()) {
			return PaymentMapper.toPaymentSuccessHistoryPageDto(Collections.emptyList(), 0);
		}

		List<Payment> payments = paymentRepository.findAllWithAssociationsByIds(paymentIds);
		Map<Long, Payment> map = payments.stream()
			.collect(Collectors.toMap(Payment::getPaymentId, p -> p));
		List<Payment> allSuccessPayments = paymentIds.stream()
			.map(map::get)
			.toList();

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
	public PaymentCancelHistoryPageDto viewAllFailureTransaction(Long userId, int page, int pageSize) {
		List<Long> paymentIds = paymentRepository.findPaymentIdsByUserAndStatus(userId,
			PaymentStatus.CANCELED.name(), (page - 1) * pageSize, pageSize);

		if (paymentIds.isEmpty()) {
			return PaymentMapper.toPaymentFailureHistoryPageDto(Collections.emptyList(), 0);
		}

		List<Payment> payments = paymentRepository.findAllWithAssociationsByIds(paymentIds);
		Map<Long, Payment> map = payments.stream()
			.collect(Collectors.toMap(Payment::getPaymentId, p -> p));
		List<Payment> allFailurePayments = paymentIds.stream()
			.map(map::get)
			.toList();

		List<PaymentCancelHistoryResponseDto> dtoList = allFailurePayments.stream()
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

	public void validateDuplicatedPayment(Lesson lesson, User user) {
		boolean alreadyPaid = paymentRepository.existsByLessonAndUserAndStatusIn(
			lesson, user, List.of(PaymentStatus.DONE, PaymentStatus.CANCELED)
		);

		if (alreadyPaid) {
			throw new BusinessException(ErrorCode.ALREADY_PAID_LESSON);
		}
	}

}