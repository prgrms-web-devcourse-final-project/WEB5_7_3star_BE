package com.threestar.trainus.domain.lesson.issue;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonWaitingRoomService {

	@Qualifier("coreRedisTemplate")
	private final StringRedisTemplate coreRedisTemplate;

	@Qualifier("mqRedisTemplate")
	private final StringRedisTemplate mqRedisTemplate;

	// 사용자 대기열 등록
	public void enqueue(String requestId, Long lessonId, Long userId, long ticketNumber) {
		// 티켓 번호를 통해 선착순 FIFO 스코어 생성
		double score = LessonApplyStreamConstant.SEQUENCE_SCORE_OFFSET - ticketNumber;

		String statusKey = LessonApplyStreamConstant.STATUS_PREFIX + requestId;

		// 상태 정보 저장 (상태:레슨ID:유저ID:진입시간)
		String waitInfo = String.format("%s:%d:%d:%d", LessonApplyStreamConstant.STATUS_WAITING, lessonId, userId, System.currentTimeMillis());

		//payload를 담은 key 생성
		mqRedisTemplate.opsForValue()
			.set(statusKey, waitInfo, Duration.ofMinutes(LessonApplyStreamConstant.STATUS_TTL_MINUTE));

		// 레슨별 Sorted Set 대기열 등록
		String waitingRoomKey = String.format(LessonApplyStreamConstant.WAITING_ROOM_KEY, lessonId);
		coreRedisTemplate.opsForZSet().add(waitingRoomKey, requestId, score);

		// 현재 활성화된 레슨 목록에 추가 (스케줄러 순회용)
		coreRedisTemplate.opsForSet().add(LessonApplyStreamConstant.DIRTY_SET_KEY, String.valueOf(lessonId));
	}

	// 사용자의 현재 대기 순번을 조회
	public Optional<Long> getRank(Long lessonId, String requestId) {
		String waitingRoomKey = String.format(LessonApplyStreamConstant.WAITING_ROOM_KEY, lessonId);
		Long rank = coreRedisTemplate.opsForZSet().rank(waitingRoomKey, requestId);
		return Optional.ofNullable(rank).map(r -> r + 1);
	}

	// 특정 레슨 대기열에서 가장 오래된 N명을 꺼내기
	public java.util.Set<String> dequeue(Long lessonId, long count) {
		String waitingRoomKey = String.format(LessonApplyStreamConstant.WAITING_ROOM_KEY, lessonId);
		return coreRedisTemplate.opsForZSet()
			.popMin(waitingRoomKey, count)
			.stream()
			.map(tuple -> tuple.getValue())
			.collect(java.util.stream.Collectors.toSet());
	}
}
