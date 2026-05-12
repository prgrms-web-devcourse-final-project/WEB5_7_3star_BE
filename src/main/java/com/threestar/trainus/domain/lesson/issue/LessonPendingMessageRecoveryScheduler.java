package com.threestar.trainus.domain.lesson.issue;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("consumer")
@Slf4j
@Component
@RequiredArgsConstructor
public class LessonPendingMessageRecoveryScheduler {

	private static final String STREAM_KEY = LessonApplyStreamConstant.STREAM_KEY;
	private static final String GROUP = LessonApplyStreamConstant.GROUP;
	private static final String RECOVERY_CONSUMER = "lesson-recovery-consumer";
	private static final Duration MIN_IDLE_TIME = Duration.ofSeconds(10);
	private static final int RECOVERY_BATCH_SIZE = 100;

	@Qualifier("mqRedisTemplate")
	private final StringRedisTemplate mqRedisTemplate;

	private final LessonApplyConsumer lessonApplyConsumer;

	@Scheduled(fixedDelay = 5000)
	@SchedulerLock(name = "LessonPendingMessageRecovery", lockAtMostFor = "4s", lockAtLeastFor = "1s")
	public void recoverPendingMessages() {
		// Pending 메시지 목록 조회
		PendingMessages pendingMessages = mqRedisTemplate.opsForStream()
			.pending(STREAM_KEY, GROUP, Range.unbounded(), RECOVERY_BATCH_SIZE);

		if (pendingMessages == null || pendingMessages.isEmpty()) {
			return;
		}

		// 일정 시간 이상 응답이 없던 메시지만 선별
		RecordId[] staleRecordIds = pendingMessages.stream()
			.filter(message -> message.getElapsedTimeSinceLastDelivery().compareTo(MIN_IDLE_TIME) >= 0)
			.map(PendingMessage::getId)
			.toArray(RecordId[]::new);

		if (staleRecordIds.length == 0) {
			return;
		}

		List<MapRecord<String, Object, Object>> claimedRecords = mqRedisTemplate.opsForStream()
			.claim(STREAM_KEY, GROUP, RECOVERY_CONSUMER, MIN_IDLE_TIME, staleRecordIds);

		if (claimedRecords == null || claimedRecords.isEmpty()) {
			return;
		}

		// 회수한 메시지를 기존 Consumer 흐름으로 재전달
		claimedRecords.stream()
			.map(this::toStringRecord)
			.forEach(lessonApplyConsumer::onMessage);
		log.warn("Recovered {} pending lesson apply messages.", claimedRecords.size());
	}

	private MapRecord<String, String, String> toStringRecord(MapRecord<String, Object, Object> record) {
		// Claim 결과를 String 기반 Record로 변환
		Map<String, String> value = new HashMap<>();
		record.getValue().forEach((key, val) -> value.put(String.valueOf(key), String.valueOf(val)));
		return MapRecord.create(STREAM_KEY, value).withId(record.getId());
	}
}
