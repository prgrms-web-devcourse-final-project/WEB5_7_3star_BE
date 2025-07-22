package com.threestar.trainus.domain.lesson.teacher.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.threestar.trainus.domain.lesson.teacher.entity.LessonParticipant;

import jakarta.transaction.Transactional;

public interface LessonParticipantRepository extends CrudRepository<LessonParticipant, Long> {

	boolean existsByLessonIdAndUserId(Long lessonId, Long userId);

	Optional<LessonParticipant> findByLessonIdAndUserId(Long lessonId, Long userId);

	long countByLessonId(Long lessonId);

	@Modifying
	@Transactional
	@Query("DELETE FROM LessonParticipant lp WHERE lp.lesson.id = :lessonId")
	void deleteByLessonId(Long lessonId);
}
