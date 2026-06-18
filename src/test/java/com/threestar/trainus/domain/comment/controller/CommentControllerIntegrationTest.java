package com.threestar.trainus.domain.comment.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threestar.trainus.domain.comment.dto.CommentCreateRequestDto;
import com.threestar.trainus.domain.comment.entity.Comment;
import com.threestar.trainus.domain.comment.repository.CommentRepository;
import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.config.security.JwtAuthenticationFilter;
import com.threestar.trainus.testsupport.JwtIntegrationTestSupport;

class CommentControllerIntegrationTest extends JwtIntegrationTestSupport {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@org.springframework.beans.factory.annotation.Autowired
	private org.springframework.test.web.servlet.MockMvc mockMvc;

	@org.springframework.beans.factory.annotation.Autowired
	private LessonRepository lessonRepository;

	@org.springframework.beans.factory.annotation.Autowired
	private UserRepository userRepository;

	@org.springframework.beans.factory.annotation.Autowired
	private CommentRepository commentRepository;

	private Long lessonId;
	private Lesson lesson;
	private User user;

	@BeforeEach
	void setUp() {
		user = userRepository.save(User.builder()
			.email("commenter@test.com")
			.password("1234")
			.nickname("댓글작성자")
			.role(UserRole.USER)
			.build());

		lesson = lessonRepository.save(Lesson.builder()
			.lessonLeader(user.getId())
			.lessonName("댓글 테스트 레슨")
			.description("설명입니다.")
			.maxParticipants(10)
			.startAt(LocalDateTime.now().plusDays(1))
			.endAt(LocalDateTime.now().plusDays(2))
			.price(0)
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
	}

	@AfterEach
	void tearDown() {
		commentRepository.deleteAllInBatch();
		lessonRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Test
	void create_comment_with_jwt_authentication() throws Exception {
		CommentCreateRequestDto request = new CommentCreateRequestDto("테스트 부모 댓글", null);

		mockMvc.perform(post("/api/v1/comments/" + lessonId)
				.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, bearerToken(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.message").value("댓글 등록 완료되었습니다"))
			.andExpect(jsonPath("$.data.userId").value(user.getId().intValue()))
			.andExpect(jsonPath("$.data.content").value("테스트 부모 댓글"));
	}

	@Test
	void delete_comment_with_jwt_authentication() throws Exception {
		Long commentId = createComment("삭제 대상 댓글");
		Comment comment = commentRepository.findById(commentId).orElseThrow();
		comment.delete();
		commentRepository.saveAndFlush(comment);

		mockMvc.perform(delete("/api/v1/comments/" + commentId)
				.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, bearerToken(user)))
			.andExpect(status().isNoContent());

		Optional<Comment> deleted = commentRepository.findById(commentId);
		assertThat(deleted).isPresent();
		assertThat(deleted.get().getDeleted()).isTrue();
	}

	@Test
	void create_comment_without_authorization_returns_unauthorized() throws Exception {
		CommentCreateRequestDto request = new CommentCreateRequestDto("토큰 없는 댓글", null);

		mockMvc.perform(post("/api/v1/comments/" + lessonId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden());
	}

	private Long createComment(String content) throws Exception {
		CommentCreateRequestDto request = new CommentCreateRequestDto(content, null);

		MvcResult result = mockMvc.perform(post("/api/v1/comments/" + lessonId)
				.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, bearerToken(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andReturn();

		return objectMapper.readTree(result.getResponse().getContentAsString())
			.path("data")
			.path("commentId")
			.asLong();
	}
}
