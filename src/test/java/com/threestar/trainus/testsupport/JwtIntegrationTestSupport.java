package com.threestar.trainus.testsupport;

import org.springframework.beans.factory.annotation.Autowired;

import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.global.config.security.JwtAuthenticationFilter;
import com.threestar.trainus.global.config.security.JwtProvider;

public abstract class JwtIntegrationTestSupport extends PostgresIntegrationTestSupport {

	@Autowired
	protected JwtProvider jwtProvider;

	// 테스트용 사용자에 대한 Bearer 토큰을 만든다.
	protected String bearerToken(User user) {
		return JwtAuthenticationFilter.BEARER_PREFIX + jwtProvider.createAccessToken(user.getId(), user.getRole().name());
	}
}
