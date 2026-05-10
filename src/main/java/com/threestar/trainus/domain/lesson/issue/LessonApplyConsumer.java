package com.threestar.trainus.domain.lesson.issue;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Profile("consumer")
@Slf4j
@Component
@RequiredArgsConstructor
public class LessonApplyConsumer implements StreamListener<String, MapRecord<String, String, String>> {

	@Qualifier("mqRedisTemplate")
	private final StringRedisTemplate mqRedisTemplate;

	@Qualifier("coreRedisTemplate")
	private final StringRedisTemplate coreRedisTemplate;

	private final LessonApplyService lessonApplyService;

	private final ConcurrentLinkedQueue<MapRecord<String, String, String>> buffer = new ConcurrentLinkedQueue<>();
	private static final int BATCH_SIZE = 1000;
	private static final String STREAM_KEY = LessonApplyStreamConstant.STREAM_KEY;
	private static final String GROUP = LessonApplyStreamConstant.GROUP;
	private long lastProcessedTime = System.currentTimeMillis();

	// 스트림 리스너의 onMessage
	@Override
	public void onMessage(MapRecord<String, String, String> message) {
		// 메세지를 받으면 레슨 ID 추출 및 Busy 카운터 증가
		String lessonIdStr = message.getValue().get("lessonId");
		if (lessonIdStr != null) {
			String key = "lesson:busy:" + lessonIdStr;
			String lastActiveKey = "lesson:busy:last_active:" + lessonIdStr;
			coreRedisTemplate.opsForValue().increment(key);
			coreRedisTemplate.expire(key, java.time.Duration.ofMinutes(10));
			coreRedisTemplate.opsForValue().set(lastActiveKey, String.valueOf(System.currentTimeMillis()), java.time.Duration.ofMinutes(10));
		}

		buffer.add(message);

		// 설정값(BATCH_SIZE)이 되면 프로세스 시작
		if (buffer.size() >= BATCH_SIZE) {
			processBuffer();
		}
	}

	// 오래 방치된 데이터 처리를 위한 스케줄링
	@Scheduled(fixedRate = 2000)
	public void scheduledProcess() {
		if (buffer.isEmpty()) {
			return;
		}

		long elapsed = System.currentTimeMillis() - lastProcessedTime;
		if (elapsed >= 3000) {
			processBuffer();
		}
	}

	private void processBuffer() {
		if (buffer.isEmpty()) {
			return;
		}

		lastProcessedTime = System.currentTimeMillis();

		List<MapRecord<String, String, String>> records = new ArrayList<>();
		while (records.size() < BATCH_SIZE) {
			MapRecord<String, String, String> record = buffer.poll();
			if (record == null) {
				break;
			}
			records.add(record);
		}

		if (records.isEmpty()) {
			return;
		}

		// 배치 처리를 위한 Redis 메세지 매핑, 파싱
		List<ApplyMessage> messages = records.stream().map(record -> {
			Map<String, String> value = record.getValue();
			return new ApplyMessage(Long.parseLong(value.get("lessonId")), Long.parseLong(value.get("userId")),
				value.get("requestId"), Long.parseLong(value.get("timestamp")));
		}).collect(Collectors.toList());

		try {
			// DB 배치 처리
			lessonApplyService.applyBatch(messages);

			// Pipelining 으로 상태 ACK 업데이트 / 스트림에서 DELETE 일괄 처리
			RecordId[] ids = records.stream().map(MapRecord::getId).toArray(RecordId[]::new);
			mqRedisTemplate.executePipelined(new org.springframework.data.redis.core.SessionCallback<Object>() {
				@Override
				public Object execute(org.springframework.data.redis.core.RedisOperations operations) {
					operations.opsForStream().acknowledge(STREAM_KEY, GROUP, ids);
					operations.opsForStream().delete(STREAM_KEY, ids);
					return null;
				}
			});
			log.info("Batch processed {} messages successfully", messages.size());
		} catch (Exception e) {
			// 실패 시 개별 메세지 실행
			log.error("Batch failed: {}. Falling back to individual processing.", e.getMessage());

			for (int i = 0; i < records.size(); i++) {
				MapRecord<String, String, String> record = records.get(i);
				ApplyMessage msg = messages.get(i);

				try {
					lessonApplyService.apply(msg.lessonId(), msg.userId(), msg.requestId(), msg.timestamp());
					mqRedisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, record.getId());
					mqRedisTemplate.opsForStream().delete(STREAM_KEY, record.getId());
				} catch (Exception ex) {
					log.error("Individual message failed: {}, Error={}", msg.requestId(), ex.getMessage());
					mqRedisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, record.getId());
					mqRedisTemplate.opsForStream().delete(STREAM_KEY, record.getId());
				}
			}
		} finally {
			// 작업 완료 후 각 레슨별로 이번 배치에서 처리한 개수만큼 Busy 카운터 감소, 마지막 처리 시점 기록
			Map<Long, Long> countsPerLesson = messages.stream()
				.collect(Collectors.groupingBy(ApplyMessage::lessonId, Collectors.counting()));

			countsPerLesson.forEach((lessonId, count) -> {
				String busyKey = "lesson:busy:" + lessonId;
				String lastActiveKey = "lesson:busy:last_active:" + lessonId;
				coreRedisTemplate.opsForValue().decrement(busyKey, count);
				coreRedisTemplate.opsForValue().set(lastActiveKey, String.valueOf(System.currentTimeMillis()), java.time.Duration.ofMinutes(10));
			});
		}
	}

	public void clearBuffer() {
		buffer.clear();
		lastProcessedTime = System.currentTimeMillis();
		log.info("Consumer buffer cleared.");
	}
}
