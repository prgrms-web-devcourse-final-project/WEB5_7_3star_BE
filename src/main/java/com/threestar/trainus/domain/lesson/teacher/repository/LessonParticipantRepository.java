package com.threestar.trainus.domain.lesson.teacher.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threestar.trainus.domain.lesson.teacher.entity.LessonParticipant;
import com.threestar.trainus.domain.lesson.teacher.entity.ParticipantStatus;

import jakarta.transaction.Transactional;

public interface LessonParticipantRepository extends JpaRepository<LessonParticipant, Long> {

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
		    SELECT lp.id
		    FROM lesson_participants lp
		    WHERE lp.lesson_id = :lessonId
		    ORDER BY lp.join_at ASC
		    LIMIT :limit OFFSET :offset
		""", nativeQuery = true)
	List<Long> findIdsByLesson(
		@Param("lessonId") Long lessonId,
		@Param("offset") int offset,
		@Param("limit") int limit
	);

	@Query("""
		    SELECT lp
		    FROM LessonParticipant lp
		    JOIN FETCH lp.user u
		    JOIN FETCH u.profile p
		    LEFT JOIN FETCH u.profileMetadata pm
		    WHERE lp.id IN :ids
		    ORDER BY lp.joinAt ASC
		""")
	List<LessonParticipant> findAllWithUserAndProfile(@Param("ids") List<Long> ids);

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
