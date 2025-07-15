package com.threestar.trainus.domain.user.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Random;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.threestar.trainus.domain.user.dto.EmailSendRequestDto;
import com.threestar.trainus.domain.user.dto.EmailSendResponseDto;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

	private final RedisTemplate<String, String> redisTemplate;
	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;
	private final SecureRandom secureRandom = new SecureRandom();

	private static final String VERIFICATION_CODE_KEY_PREFIX = "verificationCode";
	private static final String VERIFIED_KEY_PREFIX = "verified";

	private static final int VERIFICATION_CODE_EXPIRY_MINUTES = 5;
	private static final int VERIFIED_STATUS_EXPIRY_MINUTES = 30;

	public EmailSendResponseDto sendVerificationCode(EmailSendRequestDto request) {

		String email = request.email();
		String code = generateVerificationCode();

		String codeKey = VERIFICATION_CODE_KEY_PREFIX + email;

		redisTemplate.opsForValue().set(
			codeKey,
			code,
			Duration.ofMinutes(VERIFICATION_CODE_EXPIRY_MINUTES)
		);

		sendVerificationEmail(email, code);

		return new EmailSendResponseDto(email, VERIFICATION_CODE_EXPIRY_MINUTES);
	}

	public void verifyCode(String email, String inputCode) {
		String codeKey = VERIFICATION_CODE_KEY_PREFIX + email;
		String storedCode = redisTemplate.opsForValue().get(codeKey);

		if (storedCode == null) {
			throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
		}

		if (!storedCode.equals(inputCode)) {
			throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
		}

		redisTemplate.delete(codeKey);

		String verifiedKey = VERIFIED_KEY_PREFIX + email;

		redisTemplate.opsForValue().set(
			verifiedKey,
			"true",
			Duration.ofMinutes(VERIFIED_STATUS_EXPIRY_MINUTES)
		);
	}

	private String generateVerificationCode() {
		return String.format("%06d", secureRandom.nextInt(1000000));
	}

	private void sendVerificationEmail(String email, String code) {
		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

			Context context = new Context();
			context.setVariable(VERIFICATION_CODE_KEY_PREFIX, code);
			context.setVariable("email", email);

			String htmlContent = templateEngine.process(VERIFIED_KEY_PREFIX, context);

			helper.setTo(email);
			helper.setSubject("[TrainUs] 인증 코드");
			helper.setText(htmlContent, true);

			mailSender.send(mimeMessage);

		} catch (MessagingException e) {
			throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
		}
	}
}
