package com.threestar.trainus.global.config.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtProvider {

	private final SecretKey key;
	private final long accessTokenValidityInMilliseconds;
	private final long refreshTokenValidityInMilliseconds;

	public JwtProvider(@Value("${jwt.secret}") String secret,
		@Value("${jwt.access-token-validity}") long accessTokenValidityInMilliseconds,
		@Value("${jwt.refresh-token-validity}") long refreshTokenValidityInMilliseconds) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenValidityInMilliseconds = accessTokenValidityInMilliseconds;
		this.refreshTokenValidityInMilliseconds = refreshTokenValidityInMilliseconds;
	}

	// Access Token 생성
	public String createAccessToken(Long userId, String role) {
		return createToken(userId, role, accessTokenValidityInMilliseconds);
	}

	// Refresh Token 생성
	public String createRefreshToken(Long userId, String role) {
		return createToken(userId, role, refreshTokenValidityInMilliseconds);
	}

	// 공통 토큰 생성 로직
	private String createToken(Long userId, String role, long validityInMilliseconds) {
		Date now = new Date();
		Date validity = new Date(now.getTime() + validityInMilliseconds);

		return Jwts.builder()
			.subject(userId.toString())
			.claim("role", role)
			.issuedAt(now)
			.expiration(validity)
			.signWith(key)
			.compact();
	}

	// 토큰에서 Authentication 추출
	public Authentication getAuthentication(String token) {
		Claims claims = parseClaims(token);

		Long userId = Long.valueOf(claims.getSubject());
		String role = claims.get("role", String.class);
		List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

		return new UsernamePasswordAuthenticationToken(userId, null, authorities);
	}

	// 토큰 유효성 검증
	public boolean validateToken(String token) {
		try {
			Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
			return true;
		} catch (SecurityException | MalformedJwtException | SignatureException e) {
			log.error("잘못된 JWT 서명입니다: {}", e.getMessage());
		} catch (ExpiredJwtException e) {
			log.error("만료된 JWT 토큰입니다: {}", e.getMessage());
		} catch (UnsupportedJwtException e) {
			log.error("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
		} catch (IllegalArgumentException e) {
			log.error("JWT 토큰이 잘못되었습니다: {}", e.getMessage());
		}
		return false;
	}

	// 내부 클레임 파싱
	private Claims parseClaims(String token) {
		try {
			return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
		} catch (ExpiredJwtException e) {
			return e.getClaims();
		}
	}
}
