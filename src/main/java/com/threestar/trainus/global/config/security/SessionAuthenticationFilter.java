package com.threestar.trainus.global.config.security;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Slf4j
@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

	private final UserRepository userRepository;

	public SessionAuthenticationFilter(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		HttpSession session = request.getSession(false);

		if (session != null) {
			Long userId = (Long)session.getAttribute("LOGIN_USER");

			if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				// 사용자 정보 조회하여 권한 설정
				User user = userRepository.findById(userId).orElse(null);
				if (user != null) {
					List<SimpleGrantedAuthority> authorities = List.of(
						new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
					);

					log.info("사용자 인증 설정: userId={}, role={}, authorities={}",
						userId, user.getRole(), authorities);

					UsernamePasswordAuthenticationToken authToken =
						new UsernamePasswordAuthenticationToken(userId, null, authorities);

					SecurityContextHolder.getContext().setAuthentication(authToken);
				}
			}
		}

		filterChain.doFilter(request, response);
	}
}