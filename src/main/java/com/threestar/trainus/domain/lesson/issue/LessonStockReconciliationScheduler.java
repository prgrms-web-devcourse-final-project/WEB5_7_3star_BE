package com.threestar.trainus.domain.lesson.issue;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Profile("consumer")
@Component
@RequiredArgsConstructor
public class LessonStockReconciliationScheduler {

	private final LessonRepository lessonRepository;
	private final LessonApplyProducer lessonApplyProducer;

	@Qualifier("coreRedisTemplate")
	private final StringRedisTemplate coreRedisTemplate;

	@Qualifier("mqRedisTemplate")
	private final StringRedisTemplate mqRedisTemplate;

	@Scheduled(fixedRate = 30000)
	@SchedulerLock(name = "LessonStockReconciliation", lockAtMostFor = "25s", lockAtLeastFor = "20s")
	public void reconcileStock() {
		// 미처리 메세지 존재 시 연기 (MQ Redis)
		if (hasStreamLag()) {
			log.info("Stream still has pending messages or backlog in MQ. Postponing reconciliation.");
			return;
		}

		log.info("Starting Smart Stock Reconciliation (DB -> Redis)...");

		String dirtySetKey = LessonApplyStreamConstant.DIRTY_SET_KEY;

		// Dirty Set 확인 (Core Redis)
		java.util.Set<String> lessonIds = coreRedisTemplate.opsForSet().members(dirtySetKey);
		if (lessonIds == null || lessonIds.isEmpty()) {
			log.info("No lessons to reconcile.");
			return;
		}

		int processedCount = 0;
		for (String lessonIdStr : lessonIds) {
			try {
				Long lessonId = Long.valueOf(lessonIdStr);

				// DB 카운트 조회
				Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
				if (lessonOpt.isPresent()) {
					Lesson lesson = lessonOpt.get();
					int currentStock = lesson.getMaxParticipants() - (lesson.getParticipantCount() == null ? 0 :
						lesson.getParticipantCount());

					if (currentStock < 0)
						currentStock = 0;

					// Redis 재고 동기화 (Core Redis)
					lessonApplyProducer.setStock(lessonId, currentStock);

					// 동기화 완료 후 Dirty Set에서 제거 (Core Redis)
					coreRedisTemplate.opsForSet().remove(dirtySetKey, lessonIdStr);

					processedCount++;
					log.debug("Reconciled lesson [{}] stock to [{}]", lessonId, currentStock);
				}
			} catch (Exception e) {
				log.error("Failed to reconcile lesson [{}]: {}", lessonIdStr, e.getMessage());
			}
		}

		log.info("Finished Smart Stock Reconciliation for {} lessons.", processedCount);
	}

	// 미처리 메세지 확인 메서드 (MQ Redis)
	private boolean hasStreamLag() {
		try {
			// 컨슈머가 가져갔지만 아직 ACK를 안 보낸(처리 중인) 메시지 확인
			StreamInfo.XInfoGroups groups = mqRedisTemplate.opsForStream().groups(LessonApplyStreamConstant.STREAM_KEY);
			boolean hasPending = groups.stream()
				.filter(g -> LessonApplyStreamConstant.GROUP.equals(g.groupName()))
				.anyMatch(g -> g.pendingCount() > 0);

			if (hasPending)
				return true;

			// 컨슈머가 아직 가져가지 않은 메시지 확인 (Stream 전체 사이즈)
			Long size = mqRedisTemplate.opsForStream().size(LessonApplyStreamConstant.STREAM_KEY);
			return size != null && size > 0;

		} catch (Exception e) {
			// 스트림이 없거나 초기 상태일 경우
			return false;
		}
	}
}
