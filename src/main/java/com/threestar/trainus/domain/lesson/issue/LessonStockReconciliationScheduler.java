package com.threestar.trainus.domain.lesson.issue;

import java.util.Optional;

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
	private final StringRedisTemplate redisTemplate;

	@Scheduled(fixedRate = 30000)
	@SchedulerLock(name = "LessonStockReconciliation", lockAtMostFor = "25s", lockAtLeastFor = "20s")
	public void reconcileStock() {
		// 미처리 메세지 존재 시 연기
		if (hasStreamLag()) {
			log.info("Stream still has pending messages. Postponing reconciliation.");
			return;
		}

		log.info("Starting Smart Stock Reconciliation (DB -> Redis)...");

		String dirtySetKey = LessonApplyStreamConstant.DIRTY_SET_KEY;

		// Dirty Set 에서 꺼내 처리
		Long size = redisTemplate.opsForSet().size(dirtySetKey);
		if (size == null || size == 0) {
			log.info("No lessons to reconcile.");
			return;
		}

		int processedCount = 0;
		for (int i = 0; i < size; i++) {
			String lessonIdStr = redisTemplate.opsForSet().pop(dirtySetKey);
			if (lessonIdStr == null)
				break;

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

					// Redis 재고 동기화
					lessonApplyProducer.setStock(lessonId, currentStock);
					processedCount++;
					log.debug("Reconciled lesson [{}] stock to [{}]", lessonId, currentStock);
				}
			} catch (Exception e) {
				log.error("Failed to reconcile lesson [{}]: {}", lessonIdStr, e.getMessage());
			}
		}

		log.info("Finished Smart Stock Reconciliation for {} lessons.", processedCount);
	}

	// 미처리 메세지 확인 메서드
	private boolean hasStreamLag() {
		try {
			StreamInfo.XInfoGroups groups = redisTemplate.opsForStream().groups(LessonApplyStreamConstant.STREAM_KEY);
			return groups.stream()
				.filter(g -> LessonApplyStreamConstant.GROUP.equals(g.groupName()))
				.anyMatch(g -> g.pendingCount() > 0);
		} catch (Exception e) {
			return false;
		}
	}
}
