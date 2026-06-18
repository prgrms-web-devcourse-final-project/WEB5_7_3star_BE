package com.threestar.trainus.domain.file.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.global.config.security.JwtProvider;
import com.threestar.trainus.testsupport.ManualAwsIntegrationTestSupport;

@Disabled("수동 AWS S3 연동 검증용")
class S3ControllerManualIntegrationTest extends ManualAwsIntegrationTestSupport {

	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private AmazonS3 amazonS3;

	@Autowired
	private ObjectMapper objectMapper;

	private User user;

	@BeforeEach
	void setUp() {
		user = userRepository.save(User.builder()
			.email("manual-s3@test.com")
			.password("1234")
			.nickname("manual-s3")
			.role(UserRole.USER)
			.build());
	}

	@AfterEach
	void tearDown() {
		if (amazonS3.doesObjectExist(System.getenv("S3_BUCKET_NAME"), uploadedKey)) {
			amazonS3.deleteObject(System.getenv("S3_BUCKET_NAME"), uploadedKey);
		}
		userRepository.deleteAllInBatch();
	}

	private String uploadedKey;

	@Test
	void presigned_put_and_get_round_trip() throws Exception {
		String bearer = bearerToken(user, jwtProvider);
		byte[] payload = "manual s3 round trip".getBytes(StandardCharsets.UTF_8);

		MvcResult postResult = mockMvc.perform(get("/api/v1/s3/posturl")
				.param("filename", "manual-s3.txt")
				.header("Authorization", bearer))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.preSignedUrl").isNotEmpty())
			.andExpect(jsonPath("$.data.key").isNotEmpty())
			.andReturn();

		JsonNode postBody = objectMapper.readTree(postResult.getResponse().getContentAsString());
		String putUrl = postBody.path("data").path("preSignedUrl").asText();
		uploadedKey = postBody.path("data").path("key").asText();

		HttpResponse<String> putResponse = HTTP_CLIENT.send(
			HttpRequest.newBuilder(URI.create(putUrl))
				.header("Content-Type", "text/plain")
				.PUT(HttpRequest.BodyPublishers.ofByteArray(payload))
				.build(),
			HttpResponse.BodyHandlers.ofString()
		);

		assertThat(putResponse.statusCode()).isBetween(200, 299);
		assertThat(amazonS3.doesObjectExist(System.getenv("S3_BUCKET_NAME"), uploadedKey)).isTrue();

		MvcResult getResult = mockMvc.perform(get("/api/v1/s3/geturl")
				.param("key", uploadedKey)
				.header("Authorization", bearer))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.preSignedUrl").isNotEmpty())
			.andReturn();

		JsonNode getBody = objectMapper.readTree(getResult.getResponse().getContentAsString());
		String getUrl = getBody.path("data").path("preSignedUrl").asText();

		HttpResponse<byte[]> getResponse = HTTP_CLIENT.send(
			HttpRequest.newBuilder(URI.create(getUrl))
				.GET()
				.build(),
			HttpResponse.BodyHandlers.ofByteArray()
		);

		assertThat(getResponse.statusCode()).isEqualTo(200);
		assertThat(getResponse.body()).isEqualTo(payload);
	}
}
