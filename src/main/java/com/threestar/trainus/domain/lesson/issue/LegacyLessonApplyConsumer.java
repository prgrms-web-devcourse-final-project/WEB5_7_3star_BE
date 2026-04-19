package com.threestar.trainus.domain.lesson.issue;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Profile("consumer & legacy")
@Slf4j
@Component
@RequiredArgsConstructor
public class LegacyLessonApplyConsumer implements StreamListener<String, MapRecord<String, String, String>> {

	@Qualifier("mqRedisTemplate")
	private final StringRedisTemplate mqRedisTemplate;

	private final LessonApplyService lessonApplyService;

	private static final String STREAM_KEY = LessonApplyStreamConstant.STREAM_KEY;
	private static final String GROUP = LessonApplyStreamConstant.GROUP;

	@Override
	public void onMessage(MapRecord<String, String, String> message) {
		Map<String, String> value = message.getValue();

		Long lessonId = Long.parseLong(value.get("lessonId"));
		Long userId = Long.parseLong(value.get("userId"));
		String requestId = value.get("requestId");
		Long timestamp = Long.parseLong(value.get("timestamp"));

		try {
			// DB 저장
			lessonApplyService.apply(lessonId, userId, requestId, timestamp);

			// Redis ACK 및 DELETE
			mqRedisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, message.getId());
			mqRedisTemplate.opsForStream().delete(STREAM_KEY, message.getId());

			log.info("[Legacy] Consumer processed single message: {}", requestId);
		} catch (Exception e) {
			log.error("[Legacy] Consumer failed: {}. Error: {}", requestId, e.getMessage());
			mqRedisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, message.getId());
			mqRedisTemplate.opsForStream().delete(STREAM_KEY, message.getId());
		}
	}
}
