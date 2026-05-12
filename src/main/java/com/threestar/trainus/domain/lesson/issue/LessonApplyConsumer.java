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
	private static final int CHUNK_SIZE = 100;
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

		processBatch(records, messages);
	}

	private void processBatch(
		List<MapRecord<String, String, String>> records,
		List<ApplyMessage> messages
	) {
		try {
			// DB 배치 처리
			lessonApplyService.applyBatch(messages);

			ackAndDeleteAll(records);
			log.info("Batch processed {} messages successfully", messages.size());
		} catch (Exception e) {
			// 배치 실패 시 chunk 단위 재시도
			log.error("Batch failed: {}. Retrying by chunks.", e.getMessage());
			processChunks(records, messages);
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

	private void processChunks(
		List<MapRecord<String, String, String>> records,
		List<ApplyMessage> messages
	) {
		for (int from = 0; from < messages.size(); from += CHUNK_SIZE) {
			// 전체 메세지 chunk로 나누어 처리
			int to = Math.min(from + CHUNK_SIZE, messages.size());
			List<MapRecord<String, String, String>> chunkRecords = records.subList(from, to);
			List<ApplyMessage> chunkMessages = messages.subList(from, to);

			try {
				lessonApplyService.applyBatch(chunkMessages);
				ackAndDeleteAll(chunkRecords);
				log.info("Chunk processed {} messages successfully", chunkMessages.size());
			} catch (Exception e) {
				// chunk 단위에서도 실패한 경우에만 개별 메세지 처리로 전환
				log.error("Chunk failed: {}. Falling back to individual processing.", e.getMessage());
				processIndividually(chunkRecords, chunkMessages);
			}
		}
	}

	private void processIndividually(
		List<MapRecord<String, String, String>> records,
		List<ApplyMessage> messages
	) {
		for (int i = 0; i < records.size(); i++) {
			MapRecord<String, String, String> record = records.get(i);
			ApplyMessage msg = messages.get(i);

			try {
				boolean completed = lessonApplyService.apply(msg.lessonId(), msg.userId(), msg.requestId(), msg.timestamp());
				ackAndDelete(record);
				if (!completed) {
					// 비즈니스 실패
					log.warn("Individual message completed as business failure. requestId={}", msg.requestId());
				}
			} catch (Exception e) {
				// 시스템 실패 가능성 : pending 상태 유지
				log.error("Individual message failed. requestId={}, message remains pending", msg.requestId(), e);
			}
		}
	}

	public void clearBuffer() {
		buffer.clear();
		lastProcessedTime = System.currentTimeMillis();
		log.info("Consumer buffer cleared.");
	}

	private void ackAndDeleteAll(List<MapRecord<String, String, String>> records) {
		// Pipelining 으로 여러 메세지를 ACK 처리 후 Stream에서 일괄 삭제
		RecordId[] ids = records.stream().map(MapRecord::getId).toArray(RecordId[]::new);
		mqRedisTemplate.executePipelined(new org.springframework.data.redis.core.SessionCallback<Object>() {
			@Override
			public Object execute(org.springframework.data.redis.core.RedisOperations operations) {
				operations.opsForStream().acknowledge(STREAM_KEY, GROUP, ids);
				operations.opsForStream().delete(STREAM_KEY, ids);
				return null;
			}
		});
	}

	private void ackAndDelete(MapRecord<String, String, String> record) {
		mqRedisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, record.getId());
		mqRedisTemplate.opsForStream().delete(STREAM_KEY, record.getId());
	}
}
