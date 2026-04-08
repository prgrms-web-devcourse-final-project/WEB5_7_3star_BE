package com.threestar.trainus.domain.lesson.issue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("consumer")
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
		Set<String> requestIds = waitingRoomService.dequeue(lessonId, ADMIT_BATCH_SIZE);

		if (requestIds.isEmpty()) {
			// 실제 대기열이 비었는지 다시 확인
			Long remainingInWaitingRoom = coreRedisTemplate.opsForZSet().size(String.format(LessonApplyStreamConstant.WAITING_ROOM_KEY, lessonId));
			if (remainingInWaitingRoom == null || remainingInWaitingRoom == 0) {
				coreRedisTemplate.opsForSet().remove(LessonApplyStreamConstant.DIRTY_SET_KEY, String.valueOf(lessonId));
				log.info("Lesson {} admission completed. Removed from active set.", lessonId);
			}
			return;
		}

		// MGET 방식으로 벌크 GET
		List<String> statusKeys = requestIds.stream()
			.map(id -> LessonApplyStreamConstant.STATUS_PREFIX + id)
			.toList();
		List<String> statusInfos = mqRedisTemplate.opsForValue().multiGet(statusKeys);

		if (statusInfos == null) return;

		// Pipelining 방식으로 요청 상태 변경 / Stream ADD 로직 일괄 처리
		mqRedisTemplate.executePipelined(new SessionCallback<Object>() {
			@Override
			public Object execute(RedisOperations operations) {
				for (int i = 0; i < statusKeys.size(); i++) {
					String info = statusInfos.get(i);
					if (info == null) continue;

					String[] parts = info.split(":");
					if (parts.length < 3) continue;

					String requestId = requestIds.toArray(new String[0])[i];
					Long userId = Long.parseLong(parts[2]);
					Long originalTimestamp = parts.length >= 4 ? Long.parseLong(parts[3]) : System.currentTimeMillis();

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
				}
				return null;
			}
		});

		log.debug("Admitted {} users to MQ via pipeline for lesson: {}", requestIds.size(), lessonId);
	}
}
