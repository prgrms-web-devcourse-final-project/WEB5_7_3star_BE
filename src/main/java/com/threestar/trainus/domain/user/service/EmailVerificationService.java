package com.threestar.trainus.domain.user.service;

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

	public EmailSendResponseDto sendVerificationCode(EmailSendRequestDto request) {

		String email = request.email();
		String code = String.format("%06d", new Random().nextInt(1000000));

		redisTemplate.opsForValue().set("verificationCode:" + email, code, Duration.ofMinutes(5));

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

		return new EmailSendResponseDto(email, 5);
	}

	public void verifyCode(String email, String inputCode) {

		String storedCode = redisTemplate.opsForValue().get("verificationCode:" + email);

		if (storedCode == null) {
			throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
		}

		if (!storedCode.equals(inputCode)) {
			throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
		}

		redisTemplate.delete("verificationCode:" + email);
		redisTemplate.opsForValue().set("verified:" + email, "true", Duration.ofMinutes(30));
	}
}
