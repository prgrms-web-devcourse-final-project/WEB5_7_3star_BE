package com.threestar.trainus.domain.lesson.teacher.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 레슨 생성 제한 서비스
 * Redis를 사용해 강사의 레슨 생성에 쿨타임을 적용 ->1분
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LessonCreationLimitService {

	private final RedisTemplate<String, String> redisTemplate;

	// 레슨 생성 쿨타임 (1분)
	private static final Duration CREATION_COOLTIME = Duration.ofMinutes(1);

	//redis 키 접두사
	private static final String LESSON_CREATION_KEY_PREFIX = "lesson_creation_limit:";

	//레슨 생성 가능 여부 확인 + 쿨타임 설정
	public void checkAndSetCreationLimit(Long userId) {
		String key = generateRedisKey(userId);

		// 이미 쿨타임이 설정되어 있는지 확인
		if (redisTemplate.hasKey(key)) {
			Long remainingTtl = redisTemplate.getExpire(key);
			log.info("레슨 생성 제한 - 사용자 ID: {}, 남은 시간: {}초", userId, remainingTtl);
			throw new BusinessException(ErrorCode.LESSON_CREATION_TOO_FREQUENT);
		}

		// 쿨타임 설정
		redisTemplate.opsForValue().set(key, "restricted", CREATION_COOLTIME);
		log.info("레슨 생성 쿨타임 설정 - 사용자 ID: {}, 지속시간: {}분", userId, CREATION_COOLTIME.toMinutes());
	}

	// Redis 키를 생성
	private String generateRedisKey(Long userId) {
		return LESSON_CREATION_KEY_PREFIX + userId;
	}
}
