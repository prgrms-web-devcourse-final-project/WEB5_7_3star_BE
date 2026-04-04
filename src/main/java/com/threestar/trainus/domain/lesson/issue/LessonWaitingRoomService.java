package com.threestar.trainus.domain.lesson.issue;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonWaitingRoomService {

	private final StringRedisTemplate stringRedisTemplate;

	/**
	 * 사용자를 대기열에 등록하고 신청 정보를 보관합니다.
	 */
	public void enqueue(String requestId, Long lessonId, Long userId) {
		String statusKey = LessonApplyStreamConstant.STATUS_PREFIX + requestId;
		
		// 상태 정보 저장 (상태:레슨ID:유저ID)
		String waitInfo = String.format("%s:%d:%d", 
			LessonApplyStreamConstant.STATUS_WAITING, lessonId, userId);
		
		stringRedisTemplate.opsForValue()
			.set(statusKey, waitInfo, Duration.ofMinutes(LessonApplyStreamConstant.STATUS_TTL_MINUTE));

		// Sorted Set 대기열 등록 (Score: 현재 시간)
		stringRedisTemplate.opsForZSet().add(
			LessonApplyStreamConstant.WAITING_ROOM_KEY, 
			requestId, 
			System.currentTimeMillis()
		);
	}

	/**
	 * 사용자의 현재 대기 순번을 조회합니다. (0부터 시작하므로 +1)
	 */
	public Optional<Long> getRank(String requestId) {
		Long rank = stringRedisTemplate.opsForZSet().rank(LessonApplyStreamConstant.WAITING_ROOM_KEY, requestId);
		return Optional.ofNullable(rank).map(r -> r + 1);
	}

	/**
	 * 대기열에서 가장 오래된 N명을 꺼내옵니다. (스케줄러용)
	 */
	public java.util.Set<String> dequeue(long count) {
		return stringRedisTemplate.opsForZSet().popMin(LessonApplyStreamConstant.WAITING_ROOM_KEY, count)
			.stream()
			.map(tuple -> tuple.getValue())
			.collect(java.util.stream.Collectors.toSet());
	}
}
