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

	private static final String VERIFICATION_CODE_KEY_PREFIX = "verificationCode:";
	private static final String VERIFIED_KEY_PREFIX = "verified:";

	private static final String EMAIL_COOLDOWN_KEY_PREFIX = "emailSend:cooldown:";

	private static final int VERIFICATION_CODE_EXPIRY_MINUTES = 5;
	private static final int VERIFIED_STATUS_EXPIRY_MINUTES = 30;

	private static final int EMAIL_COOLDOWN_MINUTES = 1;

	public EmailSendResponseDto sendVerificationCode(EmailSendRequestDto request) {

		//이메일 발송 쿨타임을 체크하여 재발송 차단.
		checkEmailSendCooldown(request.email());

		String email = request.email();
		String code = generateVerificationCode();

		String codeKey = VERIFICATION_CODE_KEY_PREFIX + email;

		redisTemplate.opsForValue().set(
			codeKey,
			code,
			Duration.ofMinutes(VERIFICATION_CODE_EXPIRY_MINUTES)
		);

		sendVerificationEmail(email, code);

		//이메일 쿨타임 설정
		setEmailSendCooldown(email);

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

	public boolean isEmailVerified(String email) {
		String verifiedKey = VERIFIED_KEY_PREFIX + email;
		return redisTemplate.hasKey(verifiedKey);
	}

	private void checkEmailSendCooldown(String email) {
		String cooldownKey = EMAIL_COOLDOWN_KEY_PREFIX + email;

		if (redisTemplate.hasKey(cooldownKey)) {
			throw new BusinessException(ErrorCode.EMAIL_SEND_TOO_FREQUENT);
		}
	}

	private void setEmailSendCooldown(String email) {
		String cooldownKey = EMAIL_COOLDOWN_KEY_PREFIX + email;

		redisTemplate.opsForValue().set(
			cooldownKey,
			"true",
			Duration.ofMinutes(EMAIL_COOLDOWN_MINUTES)
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
			context.setVariable("verificationCode", code);
			context.setVariable("email", email);

			String htmlContent = templateEngine.process("verification", context);

			helper.setTo(email);
			helper.setSubject("[TrainUs] 인증 코드");
			helper.setText(htmlContent, true);

			mailSender.send(mimeMessage);

		} catch (MessagingException e) {
			throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
		}
	}
}
