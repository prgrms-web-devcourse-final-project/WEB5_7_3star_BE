package com.threestar.trainus.domain.lesson.issue;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("consumer")
@Slf4j
@Component
@RequiredArgsConstructor
public class LessonApplyConsumer {

	private final StringRedisTemplate redisTemplate;
	private final LessonApplyService lessonApplyService;

	private static final String STREAM_KEY = LessonApplyStreamConstant.STREAM_KEY;
	private static final String GROUP = LessonApplyStreamConstant.GROUP;
	private static final String CONSUMER = "lesson-consumer-" + UUID.randomUUID();
	private static final String STOCK_PREFIX = LessonApplyStreamConstant.STOCK_PREFIX;

	@PostConstruct
	public void init() {
		createGroupIfNotExists();
		startConsumerThread();
	}

	private void startConsumerThread() {
		Thread consumerThread = new Thread(() -> {
			while (true) {
				try {
					consumeNewMessages();
					handlePendingMessages();
				} catch (Exception e) {
					log.error("Consumer thread error: {}", e.getMessage());
				}
			}
		});

		consumerThread.setName("lesson-stream-consumer-thread");
		consumerThread.start();
	}

	private void consumeNewMessages() {
		List<MapRecord<String, Object, Object>> messages = redisTemplate.opsForStream()
			.read(Consumer.from(GROUP, CONSUMER), StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
				StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));

		if (messages == null || messages.isEmpty())
			return;

		for (MapRecord<String, Object, Object> message : messages) {
			process(message);
		}
	}

	private void handlePendingMessages() {
		PendingMessagesSummary pending = redisTemplate.opsForStream().pending(STREAM_KEY, GROUP);

		if (pending == null || pending.getTotalPendingMessages() == 0)
			return;

		List<MapRecord<String, Object, Object>> pendingList = redisTemplate.opsForStream()
			.read(Consumer.from(GROUP, CONSUMER), StreamReadOptions.empty().count(10),
				StreamOffset.create(STREAM_KEY, ReadOffset.from("0")));

		if (pendingList == null)
			return;

		for (MapRecord<String, Object, Object> message : pendingList) {
			process(message);
		}
	}

	private void process(MapRecord<String, Object, Object> message) {
		Long lessonId = null;
		Long userId = null;
		String requestId = null;
		Long timestamp = null;
		try {
			lessonId = Long.valueOf(message.getValue().get("lessonId").toString());
			userId = Long.valueOf(message.getValue().get("userId").toString());
			requestId = message.getValue().get("requestId").toString();
			timestamp = Long.valueOf(message.getValue().get("timestamp").toString());

			log.info("CONSUME lesson={} user={} requestId={}", lessonId, userId, requestId);

			boolean applied = lessonApplyService.apply(lessonId, userId, requestId, timestamp);

			if (applied) {
				redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, message.getId());
				return;
			}

			rollbackStock(lessonId);
			redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, message.getId());
		} catch (Exception e) {
			log.error("Lesson apply failed: {}, message stays in PENDING", e.getMessage());
		}
	}

	private void rollbackStock(Long lessonId) {
		String stockKey = STOCK_PREFIX + lessonId;
		redisTemplate.opsForValue().increment(stockKey);
	}

	private void createGroupIfNotExists() {
		try {
			redisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP);
			log.info("Lesson apply stream group created");
		} catch (Exception e) {
			log.info("Lesson apply stream group already exists");
		}
	}
}
