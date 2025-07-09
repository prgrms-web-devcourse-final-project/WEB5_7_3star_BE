package com.threestar.trainus.domain.lesson.admin.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threestar.trainus.domain.lesson.admin.entity.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
	// 강사가 개설한 레슨 목록 조회용
	List<Lesson> findByLessonLeaderAndDeletedAtIsNull(Long lessonLeader);

	//삭제되지 않은 레슨만 조회
	Optional<Lesson> findByIdAndDeletedAtIsNull(Long lessonId);
}
