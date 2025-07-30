package com.threestar.trainus.domain.lesson.teacher.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
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

	// 참가자 목록 조회용 메서드 추가
	@Query("""
		SELECT lp FROM LessonParticipant lp
		JOIN FETCH lp.user u
		JOIN FETCH u.profile
		WHERE lp.lesson = :lesson
		ORDER BY lp.joinAt ASC
		""")
	Page<LessonParticipant> findByLessonWithUserAndProfile(
		@Param("lesson") Lesson lesson,
		Pageable pageable
	);

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
}
