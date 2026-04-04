package com.threestar.trainus.domain.lesson.issue;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LessonAdmissionScheduler {

	private final StringRedisTemplate stringRedisTemplate;
	private final LessonWaitingRoomService waitingRoomService;

	private static final long MQ_THRESHOLD = 6000L; // MQ 임계치 (2~3초 처리량)
	private static final long ADMIT_BATCH_SIZE = 1000L; // 한 번에 입장시킬 인원

	@Scheduled(fixedDelay = 500) // 0.5초마다 실행
	public void admitUsers() {
		// 1. MQ(Stream) 잔량 확인 (O(1) 연산)
		Long streamLength = stringRedisTemplate.opsForStream().size(LessonApplyStreamConstant.STREAM_KEY);
		if (streamLength == null) return;

		// 2. 임계치를 넘으면 입장을 잠시 중단 (Admission Control)
		if (streamLength >= MQ_THRESHOLD) {
			log.debug("MQ is full (size: {}). Waiting for consumers...", streamLength);
			return;
		}

		// 3. 대기열에서 인원 추출 (ZPOPMIN)
		Set<String> requestIds = waitingRoomService.dequeue(ADMIT_BATCH_SIZE);
		if (requestIds.isEmpty()) return;

		for (String requestId : requestIds) {
			processAdmission(requestId);
		}
		
		log.info("Admitted {} users to MQ. Current Stream Size: {}", requestIds.size(), streamLength + requestIds.size());
	}

	private void processAdmission(String requestId) {
		String statusKey = LessonApplyStreamConstant.STATUS_PREFIX + requestId;
		String info = stringRedisTemplate.opsForValue().get(statusKey);

		if (info == null || !info.startsWith(LessonApplyStreamConstant.STATUS_WAITING)) {
			return;
		}

		// 정보 파싱 (STATUS:lessonId:userId)
		String[] parts = info.split(":");
		if (parts.length < 3) return;

		Long lessonId = Long.parseLong(parts[1]);
		Long userId = Long.parseLong(parts[2]);

		// 4. 상태를 PROCESSING으로 변경
		stringRedisTemplate.opsForValue().set(statusKey, LessonApplyStreamConstant.STATUS_PROCESSING, 
			Duration.ofMinutes(LessonApplyStreamConstant.STATUS_TTL_MINUTE));

		// 5. 실제 작업 큐(Stream)로 전송 (기존 MQ 로직과 연결)
		Map<String, String> content = new HashMap<>();
		content.put("lessonId", String.valueOf(lessonId));
		content.put("userId", String.valueOf(userId));
		content.put("requestId", requestId);
		content.put("timestamp", String.valueOf(System.currentTimeMillis()));

		stringRedisTemplate.opsForStream().add(LessonApplyStreamConstant.STREAM_KEY, content);
	}
}
