package com.threestar.trainus.domain.lesson.admin.repository;

import org.springframework.data.repository.CrudRepository;

import com.threestar.trainus.domain.lesson.admin.entity.LessonParticipant;

public interface LessonParticipantRepository extends CrudRepository<LessonParticipant, Long> {

	boolean existsByLessonIdAndUserId(Long lessonId, Long userId);
}
