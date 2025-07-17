package com.threestar.trainus.domain.lesson.teacher.repository;

import org.springframework.data.repository.CrudRepository;

import com.threestar.trainus.domain.lesson.teacher.entity.LessonParticipant;

public interface LessonParticipantRepository extends CrudRepository<LessonParticipant, Long> {

	boolean existsByLessonIdAndUserId(Long lessonId, Long userId);
}
