package com.threestar.trainus.domain.user.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.threestar.trainus.domain.user.dto.EmailVerificationRequestDto;
import com.threestar.trainus.domain.user.dto.EmailVerificationResponseDto;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

	private final RedisTemplate<String, String> redisTemplate;
	private final JavaMailSender mailSender;

	public EmailVerificationResponseDto sendVerificationCode(EmailVerificationRequestDto request) {

		String email = request.email();
		String code = String.format("%06d", new Random().nextInt(1000000));

		redisTemplate.opsForValue().set("code:" + email, code, Duration.ofMinutes(5));

		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(email);
		message.setSubject("[TrainUs] 인증 코드");
		message.setText("인증 코드: " + code);
		mailSender.send(message);

		return new EmailVerificationResponseDto(email, 5);
	}

	public void verifyCode(String email, String inputCode) {

		String storedCode = redisTemplate.opsForValue().get("code:" + email);

		if (storedCode == null) {
			throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
		}

		if (!storedCode.equals(inputCode)) {
			throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
		}

		redisTemplate.delete("code:" + email);
		redisTemplate.opsForValue().set("verified:" + email, "true", Duration.ofMinutes(30));
	}
}
