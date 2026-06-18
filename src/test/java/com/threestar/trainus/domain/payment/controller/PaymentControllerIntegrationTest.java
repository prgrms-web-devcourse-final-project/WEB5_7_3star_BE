package com.threestar.trainus.domain.payment.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonParticipant;
import com.threestar.trainus.domain.lesson.teacher.entity.ParticipantStatus;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonParticipantRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import com.threestar.trainus.domain.payment.dto.ConfirmPaymentRequestDto;
import com.threestar.trainus.domain.payment.dto.PaymentClient;
import com.threestar.trainus.domain.payment.dto.PaymentRequestDto;
import com.threestar.trainus.domain.payment.dto.TossPaymentResponseDto;
import com.threestar.trainus.domain.payment.dto.cancel.CancelDetail;
import com.threestar.trainus.domain.payment.dto.cancel.CancelPaymentRequestDto;
import com.threestar.trainus.domain.payment.entity.Payment;
import com.threestar.trainus.domain.payment.entity.PaymentMethod;
import com.threestar.trainus.domain.payment.entity.PaymentStatus;
import com.threestar.trainus.domain.payment.entity.TossPayment;
import com.threestar.trainus.domain.payment.repository.PaymentRepository;
import com.threestar.trainus.domain.payment.repository.TossPaymentRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.config.security.JwtAuthenticationFilter;
import com.threestar.trainus.testsupport.JwtIntegrationTestSupport;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

class PaymentControllerIntegrationTest extends JwtIntegrationTestSupport {

	private static final String SUCCESS_TIME = "2026-06-13T09:01:00+09:00";
	private static final String REQUEST_TIME = "2026-06-13T09:00:00+09:00";

	private final ObjectMapper objectMapper = new ObjectMapper();

	@org.springframework.beans.factory.annotation.Autowired
	private MockMvc mockMvc;

	@org.springframework.beans.factory.annotation.Autowired
	private UserRepository userRepository;

	@org.springframework.beans.factory.annotation.Autowired
	private LessonRepository lessonRepository;

	@org.springframework.beans.factory.annotation.Autowired
	private LessonParticipantRepository lessonParticipantRepository;

	@org.springframework.beans.factory.annotation.Autowired
	private PaymentRepository paymentRepository;

	@org.springframework.beans.factory.annotation.Autowired
	private TossPaymentRepository tossPaymentRepository;

	@MockBean
	private PaymentClient paymentClient;

	@PersistenceContext
	private EntityManager entityManager;

	private User user;
	private Lesson lesson;

	@BeforeEach
	void setUp() {
		user = userRepository.save(User.builder()
			.email("payment-user@test.com")
			.password("1234")
			.nickname("결제테스트")
			.role(UserRole.USER)
			.build());

		lesson = Lesson.builder()
			.lessonLeader(user.getId())
			.lessonName("결제 테스트 레슨")
			.description("결제 테스트 설명")
			.maxParticipants(20)
			.startAt(LocalDateTime.now().plusDays(40))
			.endAt(LocalDateTime.now().plusDays(40).plusHours(2))
			.price(30000)
			.category(Category.BADMINTON)
			.openTime(null)
			.openRun(false)
			.city("서울")
			.district("강남")
			.dong("삼성")
			.address("서울 강남구 삼성동 코엑스 3층")
			.addressDetail("코엑스 3층")
			.build();
		ReflectionTestUtils.setField(lesson, "participantCount", 1);
		lesson = lessonRepository.save(lesson);

		lessonParticipantRepository.save(LessonParticipant.builder()
			.lesson(lesson)
			.user(user)
			.build());
	}

	@AfterEach
	void tearDown() {
		tossPaymentRepository.deleteAllInBatch();
		paymentRepository.deleteAllInBatch();
		lessonParticipantRepository.deleteAllInBatch();
		lessonRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Test
	void prepare_payment_with_jwt_authentication() throws Exception {
		PaymentRequestDto request = new PaymentRequestDto(lesson.getId(), null);

		MvcResult result = mockMvc.perform(post("/api/v1/payments/prepare")
				.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, bearerToken(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("결제 정보 준비 완료"))
			.andExpect(jsonPath("$.data.lessonTitle").value("결제 테스트 레슨"))
			.andExpect(jsonPath("$.data.originPrice").value(30000))
			.andExpect(jsonPath("$.data.payPrice").value(30000))
			.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		String orderId = response.path("data").path("orderId").asText();

		Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow();
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
		assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
		assertThat(payment.getUser().getId()).isEqualTo(user.getId());
	}

	@Test
	void prepare_payment_without_authorization_returns_forbidden() throws Exception {
		PaymentRequestDto request = new PaymentRequestDto(lesson.getId(), null);

		mockMvc.perform(post("/api/v1/payments/prepare")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden());
	}

	@Test
	void confirm_payment_with_jwt_authentication() throws Exception {
		String orderId = prepareReadyPayment();
		given(paymentClient.confirmPayment(any())).willReturn(createConfirmResponse(orderId, 30000));

		ConfirmPaymentRequestDto request = new ConfirmPaymentRequestDto(30000, orderId, "payment-key");

		mockMvc.perform(post("/api/v1/payments/confirm")
				.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, bearerToken(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("결제 성공"))
			.andExpect(jsonPath("$.data.payPrice").value(30000))
			.andExpect(jsonPath("$.data.paymentMethod").value("CREDIT_CARD"));

		Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow();
		TossPayment tossPayment = tossPaymentRepository.findByOrderId(orderId).orElseThrow();
		LessonParticipant participant = lessonParticipantRepository.findByLessonIdAndUserId(lesson.getId(), user.getId())
			.orElseThrow();

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DONE);
		assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
		assertThat(tossPayment.getPaymentStatus()).isEqualTo(PaymentStatus.DONE);
		assertThat(participant.getStatus()).isEqualTo(ParticipantStatus.COMPLETED);
	}

	@Test
	void confirm_payment_without_authorization_returns_forbidden() throws Exception {
		ConfirmPaymentRequestDto request = new ConfirmPaymentRequestDto(30000, "order-id", "payment-key");

		mockMvc.perform(post("/api/v1/payments/confirm")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden());
	}

	@Test
	void cancel_payment_with_jwt_authentication() throws Exception {
		String orderId = prepareReadyPayment();
		given(paymentClient.confirmPayment(any())).willReturn(createConfirmResponse(orderId, 30000));
		mockMvc.perform(post("/api/v1/payments/confirm")
				.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, bearerToken(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new ConfirmPaymentRequestDto(30000, orderId, "payment-key"))))
			.andExpect(status().isOk());

		given(paymentClient.cancelPayment(any())).willReturn(createCancelResponse(orderId, 30000));

		mockMvc.perform(post("/api/v1/payments/cancel")
				.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, bearerToken(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new CancelPaymentRequestDto(orderId, "일정 변경"))))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("결제 취소 성공"))
			.andExpect(jsonPath("$.data.lessonName").value("결제 테스트 레슨"))
			.andExpect(jsonPath("$.data.cancelReason").value("일정 변경"));

		entityManager.clear();

		Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow();
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
		assertThat(payment.getRefundPrice()).isEqualTo(30000);
		assertThat(lessonParticipantRepository.findByLessonIdAndUserId(lesson.getId(), user.getId())).isEmpty();
	}

	@Test
	void cancel_payment_without_authorization_returns_forbidden() throws Exception {
		CancelPaymentRequestDto request = new CancelPaymentRequestDto("order-id", "일정 변경");

		mockMvc.perform(post("/api/v1/payments/cancel")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden());
	}

	private String prepareReadyPayment() throws Exception {
		PaymentRequestDto request = new PaymentRequestDto(lesson.getId(), null);

		MvcResult result = mockMvc.perform(post("/api/v1/payments/prepare")
				.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, bearerToken(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andReturn();

		return objectMapper.readTree(result.getResponse().getContentAsString())
			.path("data")
			.path("orderId")
			.asText();
	}

	private TossPaymentResponseDto createConfirmResponse(String orderId, int amount) {
		return new TossPaymentResponseDto(
			"payment-key",
			orderId,
			"결제 테스트 레슨",
			"DONE",
			REQUEST_TIME,
			SUCCESS_TIME,
			amount,
			"카드",
			List.of()
		);
	}

	private TossPaymentResponseDto createCancelResponse(String orderId, int amount) {
		return new TossPaymentResponseDto(
			"payment-key",
			orderId,
			"결제 테스트 레슨",
			"CANCELED",
			REQUEST_TIME,
			SUCCESS_TIME,
			amount,
			"카드",
			List.of(new CancelDetail("일정 변경", SUCCESS_TIME, amount, amount))
		);
	}
}
