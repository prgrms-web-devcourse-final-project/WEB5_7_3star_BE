package com.threestar.trainus.domain.lesson.teacher.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

@ExtendWith(MockitoExtension.class)
class LessonCreationLimitServiceTest {
	@Mock
	private RedisTemplate<String, String> redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private LessonCreationLimitService lessonCreationLimitService;

	@Test
	@DisplayName("첫 레슨 생성 시 제한 없이 성공한다")
	void checkAndSetCreationLimit_FirstTime_Success() {
		Long userId = 1L;
		String expectedKey = "lesson_creation_limit:" + userId;

		when(redisTemplate.hasKey(expectedKey)).thenReturn(false);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		assertThatCode(() -> lessonCreationLimitService.checkAndSetCreationLimit(userId))
			.doesNotThrowAnyException();

		verify(redisTemplate).hasKey(expectedKey);
		verify(valueOperations).set(expectedKey, "restricted", Duration.ofMinutes(1));
	}

	@Test
	@DisplayName("쿨타임이 남아있을 때 레슨 생성을 제한한다")
	void checkAndSetCreationLimit_WithinCooltime_ThrowsException() {
		Long userId = 1L;
		String expectedKey = "lesson_creation_limit:" + userId;

		when(redisTemplate.hasKey(expectedKey)).thenReturn(true);
		when(redisTemplate.getExpire(expectedKey)).thenReturn(30L); // 30초 남음

		assertThatThrownBy(() -> lessonCreationLimitService.checkAndSetCreationLimit(userId))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.LESSON_CREATION_TOO_FREQUENT);

		verify(redisTemplate).hasKey(expectedKey);
		verify(redisTemplate, never()).opsForValue();
	}
}
