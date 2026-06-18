package com.threestar.trainus.domain.file.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.config.security.JwtAuthenticationFilter;
import com.threestar.trainus.testsupport.JwtIntegrationTestSupport;

class S3ControllerIntegrationTest extends JwtIntegrationTestSupport {

	private static final String PRESIGNED_URL = "https://example.com/presigned-url";

	private final ObjectMapper objectMapper = new ObjectMapper();

	@org.springframework.beans.factory.annotation.Autowired
	private MockMvc mockMvc;

	@org.springframework.beans.factory.annotation.Autowired
	private UserRepository userRepository;

	@MockBean
	private AmazonS3 amazonS3;

	private User user;

	@BeforeEach
	void setUp() {
		user = userRepository.save(User.builder()
			.email("s3-user@test.com")
			.password("1234")
			.nickname("s3테스트")
			.role(UserRole.USER)
			.build());
	}

	@AfterEach
	void tearDown() {
		userRepository.deleteAllInBatch();
	}

	@Test
	void get_posturl_with_jwt_authentication() throws Exception {
		given(amazonS3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
			.willReturn(new URL(PRESIGNED_URL));

		MvcResult result = mockMvc.perform(get("/api/v1/s3/posturl")
				.param("filename", "profile.png")
				.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, bearerToken(user)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("업로드용 presigned url 발급 완료"))
			.andExpect(jsonPath("$.data.preSignedUrl").value(PRESIGNED_URL))
			.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		assertThat(response.path("data").path("key").asText()).matches("image/" + user.getId() + "/[a-f0-9-]+-profile\\.png");

		GeneratePresignedUrlRequest request = captureRequest();
		assertThat(request.getMethod()).isEqualTo(HttpMethod.PUT);
		assertThat(request.getBucketName()).isEqualTo("test-bucket");
		assertThat(request.getKey()).matches("image/" + user.getId() + "/[a-f0-9-]+-profile\\.png");
		assertThat(request.getExpiration()).isAfter(new Date());
		assertThat(request.getExpiration()).isBefore(Date.from(Instant.now().plus(Duration.ofMinutes(11))));
	}

	@Test
	void get_posturl_without_authorization_returns_forbidden() throws Exception {
		mockMvc.perform(get("/api/v1/s3/posturl")
				.param("filename", "profile.png"))
			.andExpect(status().isForbidden());
	}

	@Test
	void get_geturl_with_jwt_authentication() throws Exception {
		given(amazonS3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
			.willReturn(new URL(PRESIGNED_URL));

		String key = "image/" + user.getId() + "/profile.png";

		MvcResult result = mockMvc.perform(get("/api/v1/s3/geturl")
				.param("key", key)
				.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, bearerToken(user)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("조회용 presigned url 발급 완료"))
			.andExpect(jsonPath("$.data.preSignedUrl").value(PRESIGNED_URL))
			.andExpect(jsonPath("$.data.key").value(key))
			.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		assertThat(response.path("data").path("key").asText()).isEqualTo(key);

		GeneratePresignedUrlRequest request = captureRequest();
		assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
		assertThat(request.getBucketName()).isEqualTo("test-bucket");
		assertThat(request.getKey()).isEqualTo(key);
		assertThat(request.getExpiration()).isAfter(new Date());
		assertThat(request.getExpiration()).isBefore(Date.from(Instant.now().plus(Duration.ofMinutes(11))));
	}

	@Test
	void get_geturl_without_authorization_returns_forbidden() throws Exception {
		mockMvc.perform(get("/api/v1/s3/geturl")
				.param("key", "image/1/profile.png"))
			.andExpect(status().isForbidden());
	}

	private GeneratePresignedUrlRequest captureRequest() {
		@SuppressWarnings("unchecked")
		org.mockito.ArgumentCaptor<GeneratePresignedUrlRequest> captor =
			org.mockito.ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
		org.mockito.Mockito.verify(amazonS3).generatePresignedUrl(captor.capture());
		return captor.getValue();
	}
}
