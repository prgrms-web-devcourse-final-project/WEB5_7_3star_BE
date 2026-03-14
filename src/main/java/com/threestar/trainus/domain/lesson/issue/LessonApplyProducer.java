package com.threestar.trainus.domain.lesson.issue;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LessonApplyProducer {

	private final StringRedisTemplate stringRedisTemplate;

	public boolean send(Long lessonId, Long userId) {
		String stockKey = LessonApplyStreamConstant.STOCK_PREFIX + lessonId;

		// Redis 원자 연산으로 재고 차감
		Long stock = stringRedisTemplate.opsForValue().decrement(stockKey);

		// 예외 및 재고 소진 처리
		if (stock == null) {
			return false;
		}

		if (stock < 0) {
			stringRedisTemplate.opsForValue().increment(stockKey);
			return false;
		}

		// Stream 메시지 생성 및 전송
		Map<String, String> content = new HashMap<>();
		content.put("lessonId", String.valueOf(lessonId));
		content.put("userId", String.valueOf(userId));

		stringRedisTemplate.opsForStream().add(LessonApplyStreamConstant.STREAM_KEY, content);
		return true;
	}

	//레슨 잔여 재고 Redis에 초기 세팅
	public void setStock(Long lessonId, int stock) {
		String stockKey = LessonApplyStreamConstant.STOCK_PREFIX + lessonId;
		stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
	}
}
