package com.threestar.trainus.domain.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.threestar.trainus.domain.coupon.user.entity.Coupon;
import com.threestar.trainus.domain.coupon.user.entity.CouponCategory;
import com.threestar.trainus.domain.coupon.user.entity.CouponStatus;
import com.threestar.trainus.domain.coupon.user.entity.UserCoupon;
import com.threestar.trainus.domain.coupon.user.service.CouponService;
import com.threestar.trainus.domain.lesson.student.service.StudentLessonService;
import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonParticipantRepository;
import com.threestar.trainus.domain.lesson.teacher.service.AdminLessonService;
import com.threestar.trainus.domain.payment.dto.ConfirmPaymentRequestDto;
import com.threestar.trainus.domain.payment.dto.PaymentClient;
import com.threestar.trainus.domain.payment.dto.PaymentRequestDto;
import com.threestar.trainus.domain.payment.dto.PaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.TossPaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.cancel.CancelDetail;
import com.threestar.trainus.domain.payment.dto.cancel.CancelPaymentRequestDto;
import com.threestar.trainus.domain.payment.dto.cancel.CancelPaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.cancel.TossCancelRequestDto;
import com.threestar.trainus.domain.payment.dto.success.PaymentSuccessHistoryPageDto;
import com.threestar.trainus.domain.payment.dto.success.SuccessfulPaymentResponseDto;
import com.threestar.trainus.domain.payment.entity.Payment;
import com.threestar.trainus.domain.payment.entity.PaymentMethod;
import com.threestar.trainus.domain.payment.entity.PaymentStatus;
import com.threestar.trainus.domain.payment.entity.TossPayment;
import com.threestar.trainus.domain.payment.repository.PaymentRepository;
import com.threestar.trainus.domain.payment.repository.TossPaymentRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	@Mock
	private UserService userService;

	@Mock
	private PaymentClient paymentClient;

	@Mock
	private AdminLessonService lessonService;

	@Mock
	private CouponService couponService;

	@Mock
	private StudentLessonService studentLessonService;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private TossPaymentRepository tossPaymentRepository;

	@Mock
	private LessonParticipantRepository lessonParticipantRepository;

	@InjectMocks
	private PaymentService paymentService;

	@Test
	@DisplayName("쿠폰 없이 결제 준비시 READY 결제를 저장하고 원가와 결제 금액을 반환한다")
	void preparePayment_WithoutCouponCreatesReadyPayment() {
		Long userId = 1L;
		Lesson lesson = createLesson(10L, 30000, LocalDateTime.now().plusDays(3));
		User user = createUser(userId);
		given(lessonService.findLessonById(lesson.getId())).willReturn(lesson);
		given(userService.getUserById(userId)).willReturn(user);
		willDoNothing().given(studentLessonService).checkValidLessonParticipant(lesson, user);
		given(paymentRepository.existsByLessonAndUserAndStatusIn(eq(lesson), eq(user), anyList())).willReturn(false);
		given(paymentRepository.findByUserAndLessonAndUserCouponIsNullAndStatus(user, lesson, PaymentStatus.READY))
			.willReturn(Optional.empty());

		PaymentResponseDto response = paymentService.preparePayment(new PaymentRequestDto(lesson.getId(), null), userId);

		assertThat(response.lessonTitle()).isEqualTo("테스트 레슨");
		assertThat(response.originPrice()).isEqualTo(30000);
		assertThat(response.payPrice()).isEqualTo(30000);
		assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
		assertThat(response.orderId()).isNotBlank();

		ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.READY);
		assertThat(captor.getValue().getUserCoupon()).isNull();
	}

	@Test
	@DisplayName("쿠폰 없이 READY 결제가 이미 있으면 기존 주문을 반환하고 새 결제를 저장하지 않는다")
	void preparePayment_ExistingReadyWithoutCouponReturnsExisting() {
		Long userId = 1L;
		Lesson lesson = createLesson(10L, 30000, LocalDateTime.now().plusDays(3));
		User user = createUser(userId);
		Payment existing = createPayment(user, lesson, "order-1", 30000, PaymentStatus.READY, null);
		given(lessonService.findLessonById(lesson.getId())).willReturn(lesson);
		given(userService.getUserById(userId)).willReturn(user);
		willDoNothing().given(studentLessonService).checkValidLessonParticipant(lesson, user);
		given(paymentRepository.existsByLessonAndUserAndStatusIn(eq(lesson), eq(user), anyList())).willReturn(false);
		given(paymentRepository.findByUserAndLessonAndUserCouponIsNullAndStatus(user, lesson, PaymentStatus.READY))
			.willReturn(Optional.of(existing));

		PaymentResponseDto response = paymentService.preparePayment(new PaymentRequestDto(lesson.getId(), null), userId);

		assertThat(response.orderId()).isEqualTo("order-1");
		assertThat(response.payPrice()).isEqualTo(30000);
		verify(paymentRepository, never()).save(any(Payment.class));
	}

	@Test
	@DisplayName("쿠폰 결제 준비시 할인 금액을 반영해 READY 결제를 저장한다")
	void preparePayment_WithCouponAppliesDiscount() {
		Long userId = 1L;
		Long userCouponId = 99L;
		Lesson lesson = createLesson(10L, 30000, LocalDateTime.now().plusDays(3));
		User user = createUser(userId);
		UserCoupon userCoupon = createUserCoupon(user, "5000원");
		given(lessonService.findLessonById(lesson.getId())).willReturn(lesson);
		given(userService.getUserById(userId)).willReturn(user);
		willDoNothing().given(studentLessonService).checkValidLessonParticipant(lesson, user);
		given(paymentRepository.existsByLessonAndUserAndStatusIn(eq(lesson), eq(user), anyList())).willReturn(false);
		given(couponService.getValidUserCoupon(userCouponId, userId)).willReturn(userCoupon);
		given(paymentRepository.findByUserAndLessonAndUserCouponAndStatus(user, lesson, userCoupon, PaymentStatus.READY))
			.willReturn(Optional.empty());
		given(couponService.calculateDiscountedPrice(lesson.getPrice(), userCoupon)).willReturn(5000);

		PaymentResponseDto response =
			paymentService.preparePayment(new PaymentRequestDto(lesson.getId(), userCouponId), userId);

		assertThat(response.originPrice()).isEqualTo(30000);
		assertThat(response.payPrice()).isEqualTo(25000);
		verify(paymentRepository).save(argThat(payment ->
			payment.getUserCoupon() == userCoupon && payment.getPayPrice().equals(25000)
		));
	}

	@Test
	@DisplayName("이미 DONE 또는 CANCELED 결제가 있으면 결제 준비를 거부한다")
	void preparePayment_DuplicatedPaymentThrows() {
		Long userId = 1L;
		Lesson lesson = createLesson(10L, 30000, LocalDateTime.now().plusDays(3));
		User user = createUser(userId);
		given(lessonService.findLessonById(lesson.getId())).willReturn(lesson);
		given(userService.getUserById(userId)).willReturn(user);
		willDoNothing().given(studentLessonService).checkValidLessonParticipant(lesson, user);
		given(paymentRepository.existsByLessonAndUserAndStatusIn(eq(lesson), eq(user), anyList())).willReturn(true);

		assertThatThrownBy(() -> paymentService.preparePayment(new PaymentRequestDto(lesson.getId(), null), userId))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_PAID_LESSON);

		verify(paymentRepository, never()).save(any(Payment.class));
	}

	@Test
	@DisplayName("결제 승인 성공시 Payment를 DONE으로 바꾸고 참가 결제를 완료한다")
	void processConfirm_Success() {
		User user = createUser(1L);
		Lesson lesson = createLesson(10L, 30000, LocalDateTime.now().plusDays(3));
		UserCoupon userCoupon = createUserCoupon(user, "5000원");
		Payment payment = createPayment(user, lesson, "order-1", 25000, PaymentStatus.READY, userCoupon);
		TossPaymentResponseDto tossResponse = createTossResponse("order-1", 25000, "카드");
		given(paymentClient.confirmPayment(any(ConfirmPaymentRequestDto.class))).willReturn(tossResponse);
		given(paymentRepository.findByOrderId("order-1")).willReturn(Optional.of(payment));

		SuccessfulPaymentResponseDto response =
			paymentService.processConfirm(new ConfirmPaymentRequestDto(25000, "order-1", "payment-key"));

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DONE);
		assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
		assertThat(response.payPrice()).isEqualTo(25000);
		verify(studentLessonService).completeParticipantPayment(lesson.getId(), user.getId());
		verify(couponService).useCoupon(userCoupon);
		verify(tossPaymentRepository).save(any(TossPayment.class));
	}

	@Test
	@DisplayName("READY가 아닌 결제는 승인 처리할 수 없다")
	void processConfirm_NotReadyThrows() {
		User user = createUser(1L);
		Lesson lesson = createLesson(10L, 30000, LocalDateTime.now().plusDays(3));
		Payment payment = createPayment(user, lesson, "order-1", 30000, PaymentStatus.DONE, null);
		given(paymentClient.confirmPayment(any(ConfirmPaymentRequestDto.class))).willReturn(createTossResponse("order-1", 30000, "카드"));
		given(paymentRepository.findByOrderId("order-1")).willReturn(Optional.of(payment));

		assertThatThrownBy(() -> paymentService.processConfirm(new ConfirmPaymentRequestDto(30000, "order-1", "payment-key")))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PAYMENT);

		verify(tossPaymentRepository, never()).save(any(TossPayment.class));
		verify(studentLessonService, never()).completeParticipantPayment(anyLong(), anyLong());
	}

	@Test
	@DisplayName("취소 가능 시간을 넘긴 결제는 취소할 수 없다")
	void processCancel_TooLateThrows() {
		User user = createUser(1L);
		Lesson lesson = createLesson(10L, 30000, LocalDateTime.now().plusHours(12));
		Payment payment = createPayment(user, lesson, "order-1", 30000, PaymentStatus.DONE, null);
		TossPayment tossPayment = createTossPayment(payment, "order-1");
		given(tossPaymentRepository.findByOrderId("order-1")).willReturn(Optional.of(tossPayment));

		assertThatThrownBy(() -> paymentService.processCancel(new CancelPaymentRequestDto("order-1", "일정 변경")))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CANCEL_DATE);

		verify(paymentClient, never()).cancelPayment(any());
	}

	@Test
	@DisplayName("결제 취소 성공시 TossPayment와 Payment를 취소 처리하고 쿠폰을 복원한다")
	void processCancel_SuccessRestoresCoupon() {
		User user = createUser(1L);
		Lesson lesson = createLesson(10L, 30000, LocalDateTime.now().plusDays(5));
		UserCoupon userCoupon = createUserCoupon(user, "5000원");
		userCoupon.use();
		Payment payment = createPayment(user, lesson, "order-1", 25000, PaymentStatus.DONE, userCoupon);
		TossPayment tossPayment = createTossPayment(payment, "order-1");
		given(tossPaymentRepository.findByOrderId("order-1")).willReturn(Optional.of(tossPayment));
		given(paymentClient.cancelPayment(any())).willReturn(createCancelTossResponse("order-1", 7500));

		CancelPaymentResponseDto response = paymentService.processCancel(new CancelPaymentRequestDto("order-1", "일정 변경"));

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
		assertThat(payment.getRefundPrice()).isEqualTo(7500);
		assertThat(payment.getUserCoupon()).isNull();
		assertThat(response.lessonName()).isEqualTo("테스트 레슨");
		assertThat(response.cancelReason()).isEqualTo("일정 변경");
		ArgumentCaptor<TossCancelRequestDto> cancelRequestCaptor = ArgumentCaptor.forClass(TossCancelRequestDto.class);
		verify(paymentClient).cancelPayment(cancelRequestCaptor.capture());
		assertThat(cancelRequestCaptor.getValue().paymentKey()).isEqualTo("payment-key");
		assertThat(cancelRequestCaptor.getValue().cancelReason()).isEqualTo("일정 변경");
		assertThat(cancelRequestCaptor.getValue().cancelAmount()).isEqualTo(7500);
		verify(studentLessonService).cancelPayment(lesson.getId(), user.getId());
		verify(couponService).restoreCoupon(userCoupon);
		verify(tossPaymentRepository).save(tossPayment);
		verify(paymentRepository).save(payment);
	}

	@Test
	@DisplayName("성공 결제 내역이 비어 있으면 추가 조회 없이 빈 페이지를 반환한다")
	void viewAllSuccessTransaction_Empty() {
		given(paymentRepository.findPaymentIdsByUserAndStatus(1L, PaymentStatus.DONE.name(), 0, 10))
			.willReturn(List.of());

		PaymentSuccessHistoryPageDto response = paymentService.viewAllSuccessTransaction(1L, 1, 10);

		assertThat(response.successHistory()).isEmpty();
		assertThat(response.count()).isZero();
		verify(paymentRepository, never()).findAllWithAssociationsByIds(anyList());
		verify(tossPaymentRepository, never()).findAllByOrderIds(anyList());
	}

	@Test
	@DisplayName("성공 결제 내역에 대응하는 TossPayment가 없으면 결제 예외가 발생한다")
	void viewAllSuccessTransaction_MissingTossPaymentThrows() {
		User user = createUser(1L);
		Lesson lesson = createLesson(10L, 30000, LocalDateTime.now().plusDays(3));
		Payment payment = createPayment(user, lesson, "order-1", 30000, PaymentStatus.DONE, null);
		ReflectionTestUtils.setField(payment, "id", 100L);
		given(paymentRepository.findPaymentIdsByUserAndStatus(1L, PaymentStatus.DONE.name(), 0, 10))
			.willReturn(List.of(100L));
		given(paymentRepository.findAllWithAssociationsByIds(List.of(100L))).willReturn(List.of(payment));
		given(tossPaymentRepository.findAllByOrderIds(List.of("order-1"))).willReturn(List.of());

		assertThatThrownBy(() -> paymentService.viewAllSuccessTransaction(1L, 1, 10))
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PAYMENT);
	}

	private Lesson createLesson(Long lessonId, int price, LocalDateTime startAt) {
		Lesson lesson = Lesson.builder()
			.lessonLeader(99L)
			.lessonName("테스트 레슨")
			.description("레슨 설명")
			.category(Category.GYM)
			.price(price)
			.maxParticipants(10)
			.startAt(startAt)
			.endAt(startAt.plusHours(2))
			.openRun(false)
			.city("서울시")
			.district("강남구")
			.dong("역삼동")
			.address("테스트 주소")
			.addressDetail("상세 주소")
			.build();
		ReflectionTestUtils.setField(lesson, "id", lessonId);
		return lesson;
	}

	private User createUser(Long userId) {
		return User.builder()
			.id(userId)
			.email("test" + userId + "@test.com")
			.password("encoded")
			.nickname("테스트유저" + userId)
			.role(UserRole.USER)
			.build();
	}

	private Coupon createCoupon(String discountPrice) {
		LocalDateTime now = LocalDateTime.now();
		return Coupon.builder()
			.name("테스트 쿠폰")
			.discountPrice(discountPrice)
			.minOrderPrice(10000)
			.status(CouponStatus.ACTIVE)
			.quantity(100)
			.category(CouponCategory.NORMAL)
			.openAt(now.minusDays(1))
			.closeAt(now.plusDays(1))
			.expirationDate(now.plusDays(30))
			.build();
	}

	private UserCoupon createUserCoupon(User user, String discountPrice) {
		return new UserCoupon(user, createCoupon(discountPrice), LocalDateTime.now().plusDays(10));
	}

	private Payment createPayment(User user, Lesson lesson, String orderId, int payPrice, PaymentStatus status,
		UserCoupon userCoupon) {
		return Payment.builder()
			.user(user)
			.lesson(lesson)
			.userCoupon(userCoupon)
			.orderId(orderId)
			.originPrice(lesson.getPrice())
			.payPrice(payPrice)
			.payDate(LocalDateTime.now().minusDays(1))
			.status(status)
			.paymentMethod(PaymentMethod.CREDIT_CARD)
			.build();
	}

	private TossPayment createTossPayment(Payment payment, String orderId) {
		return TossPayment.builder()
			.payment(payment)
			.paymentKey("payment-key")
			.orderId(orderId)
			.amount(payment.getPayPrice())
			.orderName("테스트 레슨")
			.paymentStatus(PaymentStatus.DONE)
			.paymentMethod(PaymentMethod.CREDIT_CARD)
			.requestedAt(LocalDateTime.now().minusDays(1))
			.approvedAt(LocalDateTime.now().minusDays(1))
			.build();
	}

	private TossPaymentResponseDto createTossResponse(String orderId, int amount, String method) {
		return new TossPaymentResponseDto(
			"payment-key",
			orderId,
			"테스트 레슨",
			"DONE",
			"2026-06-13T09:00:00+09:00",
			"2026-06-13T09:01:00+09:00",
			amount,
			method,
			List.of()
		);
	}

	private TossPaymentResponseDto createCancelTossResponse(String orderId, int cancelAmount) {
		return new TossPaymentResponseDto(
			"payment-key",
			orderId,
			"테스트 레슨",
			"CANCELED",
			"2026-06-13T09:00:00+09:00",
			"2026-06-13T09:01:00+09:00",
			cancelAmount,
			"카드",
			List.of(new CancelDetail("일정 변경", "2026-06-13T10:00:00+09:00", cancelAmount, cancelAmount))
		);
	}
}
