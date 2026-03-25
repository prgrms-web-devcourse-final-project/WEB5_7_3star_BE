package com.threestar.trainus.global.config.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(
				auth -> auth.requestMatchers("/api/v1/users/**", "/api/lessons/test-auth", "/swagger-ui/**",
						"/v3/api-docs/**", "/api/v1/profiles/**", "/api/v1/lessons/**", "api/v1/coupons/**",
						"/api/v1/comments/**", "/api/v1/reviews/**", "/api/v1/admin/**", "/api/v1/rankings/**",
						"/api/v1/payments/**", "/test/**", "/health", "/actuator/**")
					.permitAll()
					.requestMatchers("/api/v1/admin/**")
					.hasRole("ADMIN")
					.anyRequest()
					.authenticated())
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.formLogin(form -> form.disable())
			.httpBasic(basic -> basic.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(
				org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
			.csrf(csrf -> csrf.disable())
			.cors(cors -> cors.configurationSource(corsConfigurationSource()));

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(
			List.of("http://localhost:3000", "http://localhost:8080", "http://localhost:8031", "http://localhost:5173",
				"http://15.165.184.145:8080", "http://15.165.184.145:3000", "http://15.165.184.145:8031",
				"http://trainus-alb-1227831319.ap-northeast-2.elb.amazonaws.com"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
