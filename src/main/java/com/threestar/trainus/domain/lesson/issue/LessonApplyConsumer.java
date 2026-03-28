package com.threestar.trainus.domain.lesson.issue;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("consumer")
@Slf4j
@Component
@RequiredArgsConstructor
public class LessonApplyConsumer implements StreamListener<String, MapRecord<String, String, String>> {

	private final StringRedisTemplate redisTemplate;
	private final LessonApplyService lessonApplyService;

	private static final String STREAM_KEY = LessonApplyStreamConstant.STREAM_KEY;
	private static final String GROUP = LessonApplyStreamConstant.GROUP;
	private static final String STOCK_PREFIX = LessonApplyStreamConstant.STOCK_PREFIX;

	@Override
	public void onMessage(MapRecord<String, String, String> message) {
		Long lessonId = null;
		Long userId = null;
		String requestId = null;
		Long timestamp = null;
		try {
			lessonId = Long.valueOf(message.getValue().get("lessonId"));
			userId = Long.valueOf(message.getValue().get("userId"));
			requestId = message.getValue().get("requestId");
			timestamp = Long.valueOf(message.getValue().get("timestamp"));

			log.info("CONSUME lesson={} user={} requestId={}", lessonId, userId, requestId);

			boolean applied = lessonApplyService.apply(lessonId, userId, requestId, timestamp);

			if (applied) {
				// DB 반영 완료 후 보정 대상 등록
				redisTemplate.opsForSet().add(LessonApplyStreamConstant.DIRTY_SET_KEY, String.valueOf(lessonId));
				redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, message.getId());
				return;
			}

			rollbackStock(lessonId);
			redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, message.getId());
		} catch (Exception e) {
			log.error("Lesson apply failed for requestId={}: {}. Message remains in PENDING", requestId,
				e.getMessage());
		}
	}

	private void rollbackStock(Long lessonId) {
		String stockKey = STOCK_PREFIX + lessonId;
		redisTemplate.opsForValue().increment(stockKey);
	}
}
