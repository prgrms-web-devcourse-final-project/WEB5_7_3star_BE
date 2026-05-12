package com.threestar.trainus.domain.lesson.issue;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Metrics;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.threestar.trainus.domain.lesson.teacher.entity.ParticipantStatus;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonStatus;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonParticipant;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonParticipantRepository;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import com.threestar.trainus.domain.user.service.UserService;
import com.threestar.trainus.global.exception.domain.ErrorCode;
import com.threestar.trainus.global.exception.handler.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonApplyService {

	private final LessonRepository lessonRepository;
	private final LessonParticipantRepository lessonParticipantRepository;
	private final UserService userService;

	@Qualifier("coreRedisTemplate")
	private final StringRedisTemplate coreRedisTemplate;

	@Qualifier("mqRedisTemplate")
	private final StringRedisTemplate mqRedisTemplate;

	private final JdbcTemplate jdbcTemplate;

	@Transactional
	public void applyBatch(List<ApplyMessage> messages) {
		if (messages.isEmpty()) {
			return;
		}

		// 레슨 정보 조회
		List<Long> lessonIds = messages.stream().map(ApplyMessage::lessonId).distinct().toList();
		Map<Long, Lesson> lessonMap = lessonRepository.findAllById(lessonIds)
			.stream()
			.collect(Collectors.toMap(Lesson::getId, l -> l));

		String sql = """
			INSERT INTO lesson_participants (lesson_id, user_id, join_at, status)
			VALUES (?, ?, NOW(), ?)
			ON CONFLICT (user_id, lesson_id) DO NOTHING
			""";
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ApplyMessage msg = messages.get(i);
				Lesson lesson = lessonMap.get(msg.lessonId());

				String status = (lesson != null && lesson.getPrice() == 0) ? ParticipantStatus.COMPLETED.name() :
					ParticipantStatus.PAYMENT_PENDING.name();

				ps.setLong(1, msg.lessonId());
				ps.setLong(2, msg.userId());
				ps.setString(3, status);
			}

			@Override
			public int getBatchSize() {
				return messages.size();
			}
		});

		Map<Long, Long> countsByLesson = messages.stream()
			.collect(Collectors.groupingBy(ApplyMessage::lessonId, Collectors.counting()));

		for (Long lessonId : countsByLesson.keySet()) {
			// Dirty Set 등록 (스케줄러 보정 요청)
			coreRedisTemplate.opsForSet().add(LessonApplyStreamConstant.DIRTY_SET_KEY, String.valueOf(lessonId));
		}

		// Pipelining 상태 업데이트 및 지표 기록 일괄 처리
		mqRedisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>)connection -> {
			for (ApplyMessage msg : messages) {
				String key = LessonApplyStreamConstant.STATUS_PREFIX + msg.requestId();
				String status = "SUCCESS";
				byte[] rawKey = key.getBytes();
				byte[] rawValue = status.getBytes();
				connection.setEx(rawKey, LessonApplyStreamConstant.STATUS_TTL_MINUTE * 60, rawValue);
			}
			return null;
		});

		long now = System.currentTimeMillis();
		for (ApplyMessage msg : messages) {
			// 지표 기록
			Metrics.counter("lesson_apply_total", "version", "mq", "result", "success").increment();

			// 개별 메시지별 레이턴시 측정
			long latency = now - msg.timestamp();
			Metrics.timer("lesson.apply.latency").record(latency, TimeUnit.MILLISECONDS);
		}
	}

	@Transactional
	public boolean apply(Long lessonId, Long userId, String requestId, Long produceTime) {
		String statusKey = LessonApplyStreamConstant.STATUS_PREFIX + requestId;
		try {
			// 멱등성 검증 (중복 신청 확인)
			if (lessonParticipantRepository.existsByLessonIdAndUserId(lessonId, userId)) {
				log.warn("Lesson apply already exists. lessonId={}, userId={}", lessonId, userId);
				updateStatus(statusKey, "SUCCESS");
				return true;
			}

			User user = userService.getUserById(userId);
			Lesson lesson = lessonRepository.findById(lessonId)
				.orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND));

			// 시간 검증
			if (LocalDateTime.now().isAfter(lesson.getEndAt())) {
				log.warn("Lesson apply failed: Already closed. lessonId={}", lessonId);
				updateStatus(statusKey, "FAIL:CLOSED");
				return false;
			}

			// 실제 DB 저장
			LessonParticipant participant = LessonParticipant.builder().lesson(lesson).user(user).build();
			lessonParticipantRepository.save(participant);

			// Dirty Set 등록 (보정 스케줄러 처리 요청)
			coreRedisTemplate.opsForSet().add(LessonApplyStreamConstant.DIRTY_SET_KEY, String.valueOf(lessonId));

			// 처리 성공 결과 저장 및 지연 시간 측정
			long latency = System.currentTimeMillis() - produceTime;
			log.info("Lesson apply success. requestId={}, latency={}ms", requestId, latency);

			// Micrometer 지표 기록
			Metrics.timer("lesson.apply.latency").record(latency, TimeUnit.MILLISECONDS);

			updateStatus(statusKey, "SUCCESS");

			return true;
		} catch (BusinessException e) {
			log.warn("Lesson apply business failed. requestId={}, errorCode={}", requestId, e.getErrorCode());
			updateStatus(statusKey, "FAIL:" + e.getErrorCode().name());
			return false;
		} catch (Exception e) {
			log.error("Failed to apply lesson in consumer. requestId={}", requestId, e);
			throw e;
		}
	}

	private void updateStatus(String key, String status) {
		mqRedisTemplate.opsForValue().set(key, status, Duration.ofMinutes(LessonApplyStreamConstant.STATUS_TTL_MINUTE));
	}
}
