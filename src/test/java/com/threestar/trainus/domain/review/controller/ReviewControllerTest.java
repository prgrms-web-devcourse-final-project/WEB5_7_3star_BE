package com.threestar.trainus.domain.review.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threestar.trainus.domain.lesson.admin.entity.Category;
import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.lesson.admin.repository.LessonRepository;
import com.threestar.trainus.domain.metadata.entity.ProfileMetadata;
import com.threestar.trainus.domain.metadata.repository.ProfileMetadataRepository;
import com.threestar.trainus.domain.review.dto.ReviewCreateRequestDto;
import com.threestar.trainus.domain.review.entity.Review;
import com.threestar.trainus.domain.review.repository.ReviewRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ReviewRepository reviewRepository;

	@Autowired
	private ProfileMetadataRepository profileMetadataRepository;

	private Long lessonId;
	private Lesson lesson;
	private User reviewer1;
	private User reviewer2;
	private User reviewee;

	@BeforeEach
	void setUp() throws Exception {
		//더미 User 생성
		reviewee = userRepository.save(User.builder()
			.email("dummy@trainus.com")
			.password("1234")
			.nickname("강사")
			.role(UserRole.USER)
			.build());

		reviewer1 = userRepository.save(User.builder()
			.email("reviewer1@test.com")
			.password("pw")
			.nickname("회원")
			.role(UserRole.USER)
			.build());

		reviewer2 = userRepository.save(User.builder()
			.email("reviewer2@test.com")
			.password("pw")
			.nickname("회원2")
			.role(UserRole.USER)
			.build());

		lesson = lessonRepository.save(Lesson.builder()
			.lessonLeader(reviewee.getId())
			.lessonName("더미 레슨")
			.description("설명입니다.")
			.maxParticipants(10)
			.startAt(LocalDateTime.now().minusDays(1))
			.endAt(LocalDateTime.now().plusDays(2))
			.price(0)
			.category(Category.BADMINTON)
			.openTime(null)
			.openRun(false)
			.city("서울")
			.district("강남")
			.dong("삼성")
			.addressDetail("코엑스 3층")
			.build());

		lessonId = lesson.getId();
	}

	@AfterEach
	void tearDown() {
		reviewRepository.deleteAll();
		lessonRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void createReview() throws Exception {

		profileMetadataRepository.save(ProfileMetadata.builder()
			.user(reviewee)
			.reviewCount(0)
			.rating(0.0f)
			.build());

		ReviewCreateRequestDto request = new ReviewCreateRequestDto();
		request.setContent("테스트 리뷰1");
		request.setRating(3.5f);
		request.setReviewImage("https://example.com/image.png");

		MockHttpSession session = new MockHttpSession();
		session.setAttribute("LOGIN_USER", reviewer1.getId());

		mockMvc.perform(post("/api/v1/reviews/" + lessonId)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.message").value("작성이 완료됐습니다."))
			.andExpect(jsonPath("$.data.content").value("테스트 리뷰1"));

		ProfileMetadata profileMetadata = profileMetadataRepository.findByUserId(reviewee.getId()).orElseThrow();
		Assertions.assertEquals(1, profileMetadata.getReviewCount());
		Assertions.assertEquals(3.5f, profileMetadata.getRating());

		ReviewCreateRequestDto request2 = new ReviewCreateRequestDto();
		request2.setContent("테스트 리뷰2");
		request2.setRating(4.5f);
		request2.setReviewImage("https://example.com/image.png");

		session.setAttribute("LOGIN_USER", reviewer2.getId());

		mockMvc.perform(post("/api/v1/reviews/" + lessonId)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request2)))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.message").value("작성이 완료됐습니다."))
			.andExpect(jsonPath("$.data.content").value("테스트 리뷰2"));

		ProfileMetadata profileMetadata2 = profileMetadataRepository.findByUserId(reviewee.getId()).orElseThrow();
		Assertions.assertEquals(2, profileMetadata2.getReviewCount());
		Assertions.assertEquals(4.0f, profileMetadata2.getRating());
	}

	@Test
	void readAll_test() throws Exception {
		reviewRepository.save(Review.builder()
			.reviewer(reviewer1)
			.reviewee(reviewee)
			.lesson(lesson)
			.content("리뷰1")
			.rating(5.0f)
			.build());

		reviewRepository.save(Review.builder()
			.reviewer(reviewer2)
			.reviewee(reviewee)
			.lesson(lesson)
			.content("리뷰1")
			.rating(4.5f)
			.build());

		mockMvc.perform(get("/api/v1/reviews/" + reviewee.getId())
				.param("page", "1")
				.param("pageSize", "10"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("조회가 완료됐습니다."));

	}

}