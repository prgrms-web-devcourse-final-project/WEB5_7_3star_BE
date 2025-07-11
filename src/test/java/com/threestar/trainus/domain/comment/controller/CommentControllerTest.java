package com.threestar.trainus.domain.comment.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threestar.trainus.domain.comment.dto.CommentCreateRequestDto;
import com.threestar.trainus.domain.comment.entity.Comment;
import com.threestar.trainus.domain.comment.repository.CommentRepository;
import com.threestar.trainus.domain.lesson.admin.entity.Category;
import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.lesson.admin.repository.LessonRepository;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;

@AutoConfigureMockMvc
@SpringBootTest
class CommentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CommentRepository commentRepository;

	private Long lessonId;
	private Long userId;
	private Lesson lesson;
	private User user;

	@BeforeEach
	void setUp() throws Exception {
		//더미 User 생성
		user = userRepository.save(User.builder()
			.email("dummy@trainus.com")
			.password("1234")
			.nickname("테스터")
			.role(UserRole.USER)
			.build());

		userId = user.getId();

		lesson = lessonRepository.save(Lesson.builder()
			.lessonLeader(userId)
			.lessonName("더미 레슨")
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
			.addressDetail("코엑스 3층")
			.build());

		lessonId = lesson.getId();
	}

	@AfterEach
	void tearDown() {
		commentRepository.deleteAll();
		lessonRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void create_comment() throws Exception {
		CommentCreateRequestDto request = new CommentCreateRequestDto();
		request.setParentCommentId(null);
		request.setContent("테스트 부모 댓글");

		MockHttpSession session = new MockHttpSession();
		session.setAttribute("LOGIN_USER", userId);

		mockMvc.perform(post("/api/v1/comments/" + lessonId)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.message").value("댓글 등록 완료되었습니다"))
			.andExpect(jsonPath("$.data.userId").value(userId.intValue()));
	}

	@Test
	void readAll_test() throws Exception {
		User user2 = userRepository.save(User.builder()
			.email("user2@trainus.com")
			.password("pw2")
			.nickname("유저2")
			.role(UserRole.USER)
			.build());

		User user3 = userRepository.save(User.builder()
			.email("user3@trainus.com")
			.password("pw3")
			.nickname("유저3")
			.role(UserRole.USER)
			.build());

		// 부모 댓글 1 - user
		Long parentId1 = createComment(null, "댓글1", lessonId, user.getId());
		createComment(parentId1, "대댓글1", lessonId, user2.getId());

		// 부모 댓글 2 - user2
		Long parentId2 = createComment(null, "댓글2", lessonId, user2.getId());

		createComment(parentId2, "대댓글2", lessonId, user3.getId());
		createComment(parentId1, "대댓글2", lessonId, user3.getId());

		// 부모 댓글 3 - user3
		Long parentId3 = createComment(null, "댓글3", lessonId, user3.getId());
		createComment(parentId3, "대댓글1", lessonId, user.getId());
		createComment(parentId2, "대댓글1", lessonId, user.getId());
		createComment(parentId3, "대댓글2", lessonId, user2.getId());

		mockMvc.perform(get("/api/v1/comments/" + lesson.getId())
				.param("page", "2")
				.param("pageSize", "5"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("댓글 조회 성공"));
		// .andExpect(jsonPath("$.data.commentCount").value(9))
		// .andExpect(jsonPath("$.data.comments.length()").value(5))
		// .andExpect(jsonPath("$.data.comments[0].content").value("댓글1"))
		// .andExpect(jsonPath("$.data.comments[0].userId").value(user.getId().intValue()));
	}

	private Long createComment(Long parentId, String content, Long lessonId, Long userId) throws Exception {
		CommentCreateRequestDto request = new CommentCreateRequestDto();
		request.setParentCommentId(parentId);
		request.setContent(content);

		MockHttpSession session = new MockHttpSession();
		session.setAttribute("LOGIN_USER", userId);

		String response = mockMvc.perform(post("/api/v1/comments/" + lessonId)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();

		// JSON 파싱해서 commentId 추출
		return objectMapper.readTree(response).path("data").path("commentId").asLong();
	}

	@Test
	void delete_comment() throws Exception {
		MockHttpSession session = new MockHttpSession();
		User writer = userRepository.save(User.builder()
			.email("writer@trainus.com")
			.password("1234")
			.nickname("작성자")
			.role(UserRole.USER)
			.build());

		session.setAttribute("LOGIN_USER", writer.getId());

		Long commentId = createComment(null, "댓글1", lessonId, writer.getId());

		mockMvc.perform(delete("/api/v1/comments/" + commentId)
				.session(session))
			.andExpect(status().isNoContent());

		Optional<Comment> deleted = commentRepository.findById(commentId);
		assertThat(deleted).isEmpty();

	}

}