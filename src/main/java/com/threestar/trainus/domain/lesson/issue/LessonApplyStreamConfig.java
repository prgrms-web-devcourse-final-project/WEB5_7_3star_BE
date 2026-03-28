package com.threestar.trainus.domain.lesson.issue;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("consumer")
@Configuration
@Slf4j
@RequiredArgsConstructor
public class LessonApplyStreamConfig {

	@Value("${spring.data.redis.stream.threads.core:2}")
	private int concurrency;

	private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
	private final LessonApplyConsumer lessonApplyConsumer;
	private final StringRedisTemplate redisTemplate;

	@PostConstruct
	public void register() {
		// 스트림 생성 보장
		ensureStreamAndGroup();

		// 설정된 스레드 개수만큼 리스너 등록
		for (int i = 0; i < concurrency; i++) {
			String consumerName = "lesson-consumer-" + i + "-" + UUID.randomUUID().toString().substring(0, 4);

			container.receive(
				Consumer.from(LessonApplyStreamConstant.GROUP, consumerName),
				StreamOffset.create(LessonApplyStreamConstant.STREAM_KEY, ReadOffset.lastConsumed()),
				lessonApplyConsumer
			);
			log.info("Parallel Lesson Consumer [{}] registered", consumerName);
		}

		if (!container.isRunning()) {
			container.start();
		}
	}

	// 스트림 생성 보장 메서드
	private void ensureStreamAndGroup() {
		String key = LessonApplyStreamConstant.STREAM_KEY;
		String group = LessonApplyStreamConstant.GROUP;

		try {
			// 스트림이 없으면 생성하고 그룹도 생성
			redisTemplate.opsForStream().createGroup(key, ReadOffset.latest(), group);
			log.info("Created Redis Stream group: {}", group);
		} catch (Exception e) {
			// 이미 존재함(BUSYGROUP) 에러는 무시
			// 그 외엔 로그 출력
			if (e.getMessage() == null || !e.getMessage().contains("BUSYGROUP")) {
				log.warn("Stream group setup info: {}", e.getMessage());
			}
		}
	}
}
