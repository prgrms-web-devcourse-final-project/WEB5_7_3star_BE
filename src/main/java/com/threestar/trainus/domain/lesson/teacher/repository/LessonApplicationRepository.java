package com.threestar.trainus.domain.lesson.teacher.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threestar.trainus.domain.lesson.teacher.entity.ApplicationStatus;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonApplication;

public interface LessonApplicationRepository extends JpaRepository<LessonApplication, Long> {
	// 신청 이력 확인
	boolean existsByLessonIdAndUserId(Long lessonId, Long userId);

	// 특정 레슨 신청 조회
	Optional<LessonApplication> findByLessonIdAndUserId(Long lessonId, Long userId);

	Page<LessonApplication> findByUserId(Long userId, Pageable pageable);

	Page<LessonApplication> findByUserIdAndStatus(Long userId, ApplicationStatus status, Pageable pageable);

	int countByLessonAndStatus(Lesson lesson, ApplicationStatus status);

	// 모든 상태 신청자 조회
	@Query("""
		SELECT la FROM LessonApplication la
		JOIN FETCH la.user u
		JOIN FETCH u.profile
		WHERE la.lesson = :lesson
		ORDER BY la.createdAt DESC
		""")
	Page<LessonApplication> findByLessonWithUserAndProfile(
		@Param("lesson") Lesson lesson,
		Pageable pageable
	);

	// 특정 상태 신청자 조회
	@Query("""
		SELECT la FROM LessonApplication la
		JOIN FETCH la.user u
		JOIN FETCH u.profile
		WHERE la.lesson = :lesson AND la.status = :status
		ORDER BY la.createdAt DESC
		""")
	Page<LessonApplication> findByLessonAndStatusWithUserAndProfile(
		@Param("lesson") Lesson lesson,
		@Param("status") ApplicationStatus status,
		Pageable pageable
	);

	// 승인된 참가자 조회 - 참가자 목록용
	@Query("""
		SELECT la FROM LessonApplication la
		JOIN FETCH la.user u
		JOIN FETCH u.profile
		WHERE la.lesson = :lesson AND la.status = 'APPROVED'
		ORDER BY la.createdAt ASC
		""")
	Page<LessonApplication> findApprovedParticipantsWithUserAndProfile(
		@Param("lesson") Lesson lesson,
		Pageable pageable
	);
}
