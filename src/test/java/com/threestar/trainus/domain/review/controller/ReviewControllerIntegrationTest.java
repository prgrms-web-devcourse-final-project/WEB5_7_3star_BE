package com.threestar.trainus.domain.review.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonParticipant;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonParticipantRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import com.threestar.trainus.domain.review.dto.ReviewCreateRequestDto;
import com.threestar.trainus.domain.review.repository.ReviewRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.config.security.JwtAuthenticationFilter;
import com.threestar.trainus.testsupport.JwtIntegrationTestSupport;

class ReviewControllerIntegrationTest extends JwtIntegrationTestSupport {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@org.springframework.beans.factory.annotation.Autowired
	private MockMvc mockMvc;

	@org.springframework.beans.factory.annotation.Autowired
	private LessonRepository lessonRepository;

	@org.springframework.beans.factory.annotation.Autowired
	private UserRepository userRepository;

	@org.springframework.beans.factory.annotation.Autowired
	private ReviewRepository reviewRepository;

	@org.springframework.beans.factory.annotation.Autowired
	private LessonParticipantRepository lessonParticipantRepository;

	private Long lessonId;
	private Lesson lesson;
	private User reviewee;
	private User reviewer;

	@BeforeEach
	void setUp() {
		reviewee = userRepository.save(User.builder()
			.email("reviewee@test.com")
			.password("1234")
			.nickname("리뷰대상")
			.role(UserRole.USER)
			.build());

		reviewer = userRepository.save(User.builder()
			.email("reviewer@test.com")
			.password("1234")
			.nickname("리뷰작성자")
			.role(UserRole.USER)
			.build());

		lesson = lessonRepository.save(Lesson.builder()
			.lessonLeader(reviewee.getId())
			.lessonName("리뷰 테스트 레슨")
			.description("설명입니다.")
			.maxParticipants(10)
			.startAt(LocalDateTime.now().minusDays(4))
			.endAt(LocalDateTime.now().minusDays(1))
			.price(1000)
			.category(Category.BADMINTON)
			.openTime(null)
			.openRun(false)
			.city("서울")
			.district("강남")
			.dong("삼성")
			.address("서울 강남구 삼성동 코엑스 3층")
			.addressDetail("코엑스 3층")
			.build());

		lessonId = lesson.getId();

		lessonParticipantRepository.save(LessonParticipant.builder()
			.lesson(lesson)
			.user(reviewer)
			.build());
	}

	@AfterEach
	void tearDown() {
		lessonParticipantRepository.deleteAllInBatch();
		reviewRepository.deleteAllInBatch();
		lessonRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Test
	void create_review_with_jwt_authentication() throws Exception {
		ReviewCreateRequestDto request = new ReviewCreateRequestDto("테스트 리뷰", 3.5D, "https://example.com/image.png");

		mockMvc.perform(post("/api/v1/reviews/" + lessonId)
				.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, bearerToken(reviewer))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.message").value("작성이 완료됐습니다."))
			.andExpect(jsonPath("$.data.content").value("테스트 리뷰"))
			.andExpect(jsonPath("$.data.rating").value(3.5D));
	}

	@Test
	void create_review_without_authorization_returns_unauthorized() throws Exception {
		ReviewCreateRequestDto request = new ReviewCreateRequestDto("토큰 없는 리뷰", 4.0D, "https://example.com/image.png");

		mockMvc.perform(post("/api/v1/reviews/" + lessonId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden());
	}

	@Test
	void create_review_with_invalid_jwt_returns_unauthorized() throws Exception {
		ReviewCreateRequestDto request = new ReviewCreateRequestDto("잘못된 토큰 리뷰", 4.0D, "https://example.com/image.png");

		mockMvc.perform(post("/api/v1/reviews/" + lessonId)
				.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + "invalid.token.value")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden());
	}

}
