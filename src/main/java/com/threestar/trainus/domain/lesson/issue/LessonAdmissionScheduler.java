package com.threestar.trainus.domain.lesson.issue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("consumer & !legacy")
@Slf4j
@Component
@RequiredArgsConstructor
public class LessonAdmissionScheduler {

	@Qualifier("coreRedisTemplate")
	private final StringRedisTemplate coreRedisTemplate;

	@Qualifier("mqRedisTemplate")
	private final StringRedisTemplate mqRedisTemplate;

	private final LessonWaitingRoomService waitingRoomService;

	private static final long MQ_THRESHOLD = 8000L;
	private static final long ADMIT_BATCH_SIZE = 2000L;

	@Scheduled(fixedDelay = 100)
	public void admitUsers() {
		Set<String> activeLessonIds = coreRedisTemplate.opsForSet().members(LessonApplyStreamConstant.DIRTY_SET_KEY);

		if (activeLessonIds == null || activeLessonIds.isEmpty()) {
			return;
		}

		for (String lessonIdStr : activeLessonIds) {
			Long lessonId = Long.parseLong(lessonIdStr);
			processAdmissionForLesson(lessonId);
		}
	}

	private void processAdmissionForLesson(Long lessonId) {
		// Stream 사이즈가 설정값(MQ_THRESHOLD)보다 크면 추가하지 않음
		Long streamLength = mqRedisTemplate.opsForStream().size(LessonApplyStreamConstant.STREAM_KEY);
		if (streamLength != null && streamLength >= MQ_THRESHOLD) {
			log.debug("MQ is full (size: {}). Admission paused for lesson: {}", streamLength, lessonId);
			return;
		}

		// 해당 레슨 대기열에서 인원 추출
		List<String> requestIds = waitingRoomService.dequeue(lessonId, ADMIT_BATCH_SIZE);

		if (requestIds.isEmpty()) {
			return;
		}

		// MGET 방식으로 벌크 GET
		List<String> statusKeys = requestIds.stream()
			.map(id -> LessonApplyStreamConstant.STATUS_PREFIX + id)
			.toList();
		List<String> statusInfos = mqRedisTemplate.opsForValue().multiGet(statusKeys);

		if (statusInfos == null) {
			log.warn(
				"Admission diagnostics. lessonId={}, dequeuedCount={}, statusKeyCount={}, statusInfos=null. Admission skipped after dequeue.",
				lessonId, requestIds.size(), statusKeys.size());
			return;
		}

		AtomicInteger statusNullCount = new AtomicInteger();
		AtomicInteger invalidStatusCount = new AtomicInteger();
		AtomicInteger xaddAttemptCount = new AtomicInteger();

		// Pipelining 방식으로 요청 상태 변경 / Stream ADD 로직 일괄 처리
		mqRedisTemplate.executePipelined(new SessionCallback<Object>() {
			@Override
			public Object execute(RedisOperations operations) {
				for (int i = 0; i < statusKeys.size(); i++) {
					String info = statusInfos.get(i);
					if (info == null) {
						statusNullCount.incrementAndGet();
						continue;
					}

					String[] parts = info.split(":");
					if (parts.length < 3) {
						invalidStatusCount.incrementAndGet();
						continue;
					}

					String requestId = requestIds.get(i);
					Long userId;
					Long originalTimestamp;
					try {
						userId = Long.parseLong(parts[2]);
						originalTimestamp = parts.length >= 4 ? Long.parseLong(parts[3]) : System.currentTimeMillis();
					} catch (NumberFormatException e) {
						invalidStatusCount.incrementAndGet();
						continue;
					}

					// 상태 변경 (SET)
					String statusKey = statusKeys.get(i);
					String processingInfo = String.format("%s:%d:%d:%d", LessonApplyStreamConstant.STATUS_PROCESSING, lessonId, userId, originalTimestamp);
					operations.opsForValue().set(statusKey, processingInfo, java.time.Duration.ofMinutes(LessonApplyStreamConstant.STATUS_TTL_MINUTE));

					// 스트림 추가 (XADD)
					Map<String, String> content = new HashMap<>();
					content.put("lessonId", String.valueOf(lessonId));
					content.put("userId", String.valueOf(userId));
					content.put("requestId", requestId);
					content.put("timestamp", String.valueOf(originalTimestamp));
					operations.opsForStream().add(LessonApplyStreamConstant.STREAM_KEY, content);
					xaddAttemptCount.incrementAndGet();
				}
				return null;
			}
		});

		log.info(
			"Admission diagnostics. lessonId={}, dequeuedCount={}, statusKeyCount={}, statusNullCount={}, invalidStatusCount={}, xaddAttemptCount={}",
			lessonId, requestIds.size(), statusKeys.size(), statusNullCount.get(), invalidStatusCount.get(),
			xaddAttemptCount.get());
		log.debug("Admitted {} users to MQ via pipeline for lesson: {}", requestIds.size(), lessonId);
	}
}
