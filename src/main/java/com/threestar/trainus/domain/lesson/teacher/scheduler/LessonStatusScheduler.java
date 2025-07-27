package com.threestar.trainus.domain.lesson.teacher.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonStatus;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LessonStatusScheduler {

	private final LessonRepository lessonRepository;

	/**
	 * 1분마다 레슨 상태를 확인하여 업데이트를 진행
	 * - 모집중/모집완료 → 진행중 (시작 시간 도달)
	 * - 진행중 → 완료 (종료 시간 도달)
	 */
	@Scheduled(cron = "0 * * * * *") //1분마다 실행
	@Transactional
	public void updateLessonStatus() {
		LocalDateTime now = LocalDateTime.now();

		try {
			// 모집중/모집완료 → 진행중(시작시간 도달)
			startLessons(now);

			// 진행중 → 완료(종료시간 도달)
			completeLessons(now);

		} catch (Exception e) {
			log.error("레슨 상태 업데이트 중 오류 발생", e);
		}
	}

	private void startLessons(LocalDateTime now) {
		// 시작 시간이 지났지만 아직 모집중이거나 모집완료 상태인 레슨들 조회
		List<Lesson> lessonsToStart = lessonRepository.findLessonsToStart(now);

		if (!lessonsToStart.isEmpty()) {
			for (Lesson lesson : lessonsToStart) {
				lesson.updateStatus(LessonStatus.IN_PROGRESS);
				log.info("레슨 시작: ID={}, 이름={}, 시작시간={}",
					lesson.getId(), lesson.getLessonName(), lesson.getStartAt());
			}

			lessonRepository.saveAll(lessonsToStart);
			log.info("총 {}개의 레슨이 진행중 상태로 변경되었습니다.", lessonsToStart.size());
		}
	}

	private void completeLessons(LocalDateTime now) {
		// 종료 시간이 지났지만 아직 진행중 상태인 레슨들 조회
		List<Lesson> lessonsToComplete = lessonRepository.findLessonsToComplete(now);

		if (!lessonsToComplete.isEmpty()) {
			for (Lesson lesson : lessonsToComplete) {
				lesson.updateStatus(LessonStatus.COMPLETED);
				log.info("레슨 완료: ID={}, 이름={}, 종료시간={}",
					lesson.getId(), lesson.getLessonName(), lesson.getEndAt());
			}

			lessonRepository.saveAll(lessonsToComplete);
			log.info("총 {}개의 레슨이 완료 상태로 변경되었습니다.", lessonsToComplete.size());
		}
	}
}
