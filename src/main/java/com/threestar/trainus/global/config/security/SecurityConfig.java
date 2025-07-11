package com.threestar.trainus.global.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.threestar.trainus.global.security.SessionAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final SessionAuthenticationFilter sessionAuthenticationFilter;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/v1/users/**", "/api/lessons/test-auth", "/swagger-ui/**", "/v3/api-docs/**",
					"/api/v1/profiles/**")
				.permitAll()
				.anyRequest()
				.authenticated()
			)
			.addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.formLogin(form -> form.disable())        // 폼 로그인 비활성화
			.httpBasic(basic -> basic.disable())      // HTTP Basic 비활성화
			.sessionManagement(session -> session
				.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
			)
			.csrf(csrf -> csrf.disable())
			.cors(cors -> {});

		return http.build();
	}
}
