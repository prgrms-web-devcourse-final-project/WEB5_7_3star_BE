package com.threestar.trainus.domain.lesson.admin.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.threestar.trainus.domain.lesson.admin.entity.ApplicationStatus;
import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.lesson.admin.entity.LessonApplication;

public interface LessonApplicationRepository extends JpaRepository<LessonApplication, Long> {

	// 특정 레슨의 신청자 목록 조회
	List<LessonApplication> findByLesson(Lesson lesson);

	// 특정 레슨의 특정 상태 신청자 목록 조회
	List<LessonApplication> findByLessonAndStatus(Lesson lesson, ApplicationStatus status);

	// 페이징 지원 메서드 추가
	Page<LessonApplication> findByLesson(Lesson lesson, Pageable pageable);

	Page<LessonApplication> findByLessonAndStatus(Lesson lesson, ApplicationStatus status, Pageable pageable);
}



