package com.threestar.trainus.domain.lesson.issue;

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

	private final StringRedisTemplate redisTemplate;
	private final LessonApplyService lessonApplyService;

	private final ConcurrentLinkedQueue<MapRecord<String, String, String>> buffer = new ConcurrentLinkedQueue<>();
	private static final int BATCH_SIZE = 1000;
	private static final String STREAM_KEY = LessonApplyStreamConstant.STREAM_KEY;
	private static final String GROUP = LessonApplyStreamConstant.GROUP;

	@Override
	public void onMessage(MapRecord<String, String, String> message) {
		buffer.add(message);

		// 버퍼가 차면 즉시 처리 시도
		if (buffer.size() >= BATCH_SIZE) {
			processBuffer();
		}
	}

	// 주기적으로 버퍼에 남은 메시지 처리
	@Scheduled(fixedRate = 1000)
	public void scheduledProcess() {
		if (!buffer.isEmpty()) {
			processBuffer();
		}
	}

	private void processBuffer() {
		if (buffer.isEmpty()) {
			return;
		}
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
		List<ApplyMessage> messages = records.stream().map(record -> {
			Map<String, String> value = record.getValue();
			return new ApplyMessage(Long.parseLong(value.get("lessonId")), Long.parseLong(value.get("userId")),
				value.get("requestId"), Long.parseLong(value.get("timestamp")));
		}).collect(Collectors.toList());

		try {
			// 서비스 레이어 배치 처리 호출
			lessonApplyService.applyBatch(messages);

			// 처리 성공 시 ACK 및 메시지 삭제
			RecordId[] ids = records.stream().map(MapRecord::getId).toArray(RecordId[]::new);
			redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, ids);
			redisTemplate.opsForStream().delete(STREAM_KEY, ids);

			log.info("Batch processed {} messages successfully", messages.size());
		} catch (Exception e) {
			log.error("Batch failed due to: {}. Falling back to individual processing to isolate the error.",
				e.getMessage());

			// 배치 실패 시 한 건씩 개별 처리
			for (int i = 0; i < records.size(); i++) {
				MapRecord<String, String, String> record = records.get(i);
				ApplyMessage msg = messages.get(i);

				try {
					// 개별 처리 호출
					lessonApplyService.apply(msg.lessonId(), msg.userId(), msg.requestId(), msg.timestamp());

					// 개별 성공 시 즉시 ACK 및 삭제
					redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, record.getId());
					redisTemplate.opsForStream().delete(STREAM_KEY, record.getId());
				} catch (Exception ex) {
					// 위반 발생 시 메시지 버림
					log.error("Failed to process individual message: LessonId={}, UserId={}, Error={}",
						msg.lessonId(), msg.userId(), ex.getMessage());

					// 스트림에서 제거
					redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, record.getId());
					redisTemplate.opsForStream().delete(STREAM_KEY, record.getId());
				}
			}
		}
	}
}
