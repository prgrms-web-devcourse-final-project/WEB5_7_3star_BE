package com.threestar.trainus.domain.lesson.issue;

import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LessonApplyProducer {

	private final StringRedisTemplate stringRedisTemplate;
	private final LessonWaitingRoomService waitingRoomService;

	public String send(Long lessonId, Long userId) {
		String duplicateKey = LessonApplyStreamConstant.DUPLICATE_PREFIX + lessonId;
		String stockKey = LessonApplyStreamConstant.STOCK_PREFIX + lessonId;

		// 중복 신청 방지 (Redis Set 원자 연산)
		Long addedCount = stringRedisTemplate.opsForSet().add(duplicateKey, String.valueOf(userId));

		if (addedCount == null) {
			Metrics.counter("lesson_apply_total", "version", "mq", "result", "system_error").increment();
			return null;
		}

		if (addedCount == 0L) {
			Metrics.counter("lesson_apply_total", "version", "mq", "result", "duplicate_reject").increment();
			return "ALREADY_APPLIED";
		}

		// 최초 신청 시에만 TTL 설정
		stringRedisTemplate.expire(duplicateKey, Duration.ofMinutes(LessonApplyStreamConstant.DUPLICATE_TTL_MINUTE));

		// Redis 원자 연산으로 재고 차감
		Long stock = stringRedisTemplate.opsForValue().decrement(stockKey);

		// 예외 및 재고 소진 처리
		if (stock == null) {
			stringRedisTemplate.opsForSet().remove(duplicateKey, String.valueOf(userId));
			Metrics.counter("lesson_apply_total", "version", "mq", "result", "system_error").increment();
			return null;
		}

		if (stock < 0) {
			Metrics.counter("lesson_apply_total", "version", "mq", "result", "pre_filter_reject").increment();
			stringRedisTemplate.opsForValue().increment(stockKey);
			stringRedisTemplate.opsForSet().remove(duplicateKey, String.valueOf(userId));
			return null;
		}

		// requestId 생성 및 대기열 등록 (Phase 1)
		String requestId = UUID.randomUUID().toString();
		waitingRoomService.enqueue(requestId, lessonId, userId);

		Metrics.counter("lesson_apply_total", "version", "mq", "result", "applied").increment();
		return requestId;
	}

	//레슨 잔여 재고 Redis에 초기 세팅
	public void setStock(Long lessonId, int stock) {
		String stockKey = LessonApplyStreamConstant.STOCK_PREFIX + lessonId;
		stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
	}
}
