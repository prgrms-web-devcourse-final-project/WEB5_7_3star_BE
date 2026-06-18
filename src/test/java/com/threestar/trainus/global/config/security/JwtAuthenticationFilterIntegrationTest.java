package com.threestar.trainus.global.config.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;
import com.threestar.trainus.domain.user.repository.UserRepository;
import com.threestar.trainus.testsupport.PostgresIntegrationTestSupport;

/*
 * JWT 인증 필터가 실제 Spring Security filter chain에서 동작하는지 확인하는 smoke 통합 테스트다.
 * 유효한 Bearer token 인증 성공과 일반 사용자 token의 관리자 경로 접근 거부를 MockMvc로 검증한다.
 */
class JwtAuthenticationFilterIntegrationTest extends PostgresIntegrationTestSupport {

	// Docker/Testcontainers 연결 이슈는 PostgresIntegrationTestSupport에 정리되어 있다.
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private UserRepository userRepository;

	@AfterEach
	void clearDatabase() {
		userRepository.deleteAllInBatch();
	}

	@Test
	@DisplayName("JWT 필터 체인: 유효한 Bearer token은 인증된 사용자 요청을 통과시킨다")
	void validJwtAuthenticatesRequest() throws Exception {
		User user = userRepository.save(User.builder()
			.email("jwt-user@test.com")
			.password("password")
			.nickname("jwt-user")
			.role(UserRole.USER)
			.build());
		String token = jwtProvider.createAccessToken(user.getId(), user.getRole().name());

		mockMvc.perform(get("/api/v1/users/me")
				.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + token))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("JWT 필터 체인: 일반 사용자 토큰은 관리자 경로 접근이 거부된다")
	void userJwtCannotAccessAdminRoute() throws Exception {
		User user = userRepository.save(User.builder()
			.email("jwt-denied-user@test.com")
			.password("password")
			.nickname("jwt-denied-user")
			.role(UserRole.USER)
			.build());
		String token = jwtProvider.createAccessToken(user.getId(), user.getRole().name());

		mockMvc.perform(get("/api/v1/admin/coupons")
				.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + token))
			.andExpect(status().isForbidden());
	}
}
