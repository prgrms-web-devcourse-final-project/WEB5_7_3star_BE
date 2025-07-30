package com.threestar.trainus.domain.lesson.teacher.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.threestar.trainus.domain.lesson.teacher.entity.LessonParticipant;
import com.threestar.trainus.domain.lesson.teacher.entity.ParticipantStatus;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;

public interface LessonParticipantRepository extends CrudRepository<LessonParticipant, Long> {

	boolean existsByLessonIdAndUserId(Long lessonId, Long userId);

	Optional<LessonParticipant> findByLessonIdAndUserId(Long lessonId, Long userId);

	long countByLessonId(Long lessonId);

	@Modifying
	@Transactional
	@Query("DELETE FROM LessonParticipant lp WHERE lp.lesson.id = :lessonId")
	void deleteByLessonId(Long lessonId);

	// 결제 대기 상태인 참가자 조회 (결제 검증용)
	@Query("""
		SELECT lp FROM LessonParticipant lp
		WHERE lp.lesson.id = :lessonId AND lp.user.id = :userId
		AND lp.status = :status
		""")
	Optional<LessonParticipant> findByLessonIdAndUserIdAndStatus(
		@Param("lessonId") Long lessonId,
		@Param("userId") Long userId,
		@Param("status") ParticipantStatus status
	);

	@Query(value = """
		    SELECT lp.*
		    FROM lesson_participants lp
		    JOIN user u ON lp.user_id = u.id
		    JOIN profile p ON u.id = p.user_id
		    WHERE lp.lesson_id = :lessonId
		    ORDER BY lp.join_at ASC
		    LIMIT :limit OFFSET :offset
		""", nativeQuery = true)
	List<LessonParticipant> findAllByLesson(
		@Param("lessonId") Long lessonId,
		@Param("offset") int offset,
		@Param("limit") int limit
	);

	@Query(value = """
		    SELECT COUNT(*) FROM (
		        SELECT lp.id
		        FROM lesson_participants lp
		        WHERE lp.lesson_id = :lessonId
		        LIMIT :limit
		    ) t
		""", nativeQuery = true)
	int countAllByLesson(
		@Param("lessonId") Long lessonId,
		@Param("limit") int limit
	);
}
