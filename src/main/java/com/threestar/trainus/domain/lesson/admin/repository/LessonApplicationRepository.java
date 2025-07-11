package com.threestar.trainus.domain.lesson.admin.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.threestar.trainus.domain.lesson.admin.entity.ApplicationStatus;
import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.lesson.admin.entity.LessonApplication;

public interface LessonApplicationRepository extends JpaRepository<LessonApplication, Long> {

	// 특정 레슨의 신청자 목록 조회(페이징)
	Page<LessonApplication> findByLesson(Lesson lesson, Pageable pageable);

	//특정 레슨의 특정 상태 신청자 목록 조회(페이징)
	Page<LessonApplication> findByLessonAndStatus(Lesson lesson, ApplicationStatus status, Pageable pageable);

	// 신청 이력 확인
	boolean existsByLessonIdAndUserId(Long lessonId, Long userId);

	// 특정 레슨 신청 조회
	Optional<LessonApplication> findByLessonIdAndUserId(Long lessonId, Long userId);
}



