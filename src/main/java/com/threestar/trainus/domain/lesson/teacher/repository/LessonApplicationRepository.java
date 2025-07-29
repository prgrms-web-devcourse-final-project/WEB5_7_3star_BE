package com.threestar.trainus.domain.lesson.teacher.repository;

import java.util.List;
import java.util.Optional;

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

	List<LessonApplication> findByUserId(Long userId);

	int countByLessonAndStatus(Lesson lesson, ApplicationStatus status);

	@Query(value = """
		   	select *
		   	from lesson_applications la
		   	where la.user_id = :userId
		     	and (:status is null or la.status = :status)
		   	order by la.created_at desc
		   	limit :limit offset :offset
		""", nativeQuery = true)
	List<LessonApplication> findAllByUserAndStatus(@Param("userId") Long userId, @Param("status") String status,
		@Param("offset") int offset, @Param("limit") int limit);

	@Query(value = """
		   select count(*)
		   from (
		       select la.id
		       from lesson_applications la
		       where la.user_id = :userId
		         and (:status is null or la.status = :status)
		       limit :limit
		   ) t
		""", nativeQuery = true)
	int countByUserAndStatus(@Param("userId") Long userId, @Param("status") String status, @Param("limit") int limit);

	// 모든 상태 신청자 조회
	@Query(value = """
		    SELECT la.*
		    FROM lesson_applications la
		    LEFT JOIN user u ON la.user_id = u.id
		    LEFT JOIN profile p ON u.id = p.user_id
		    WHERE la.lesson_id = :lessonId
		    ORDER BY la.created_at DESC
		    LIMIT :limit OFFSET :offset
		""", nativeQuery = true)
	List<LessonApplication> findAllByLesson(@Param("lessonId") Long lessonId, @Param("offset") int offset,
		@Param("limit") int limit);

	// 모든 상태 신청자 count (최대 countLimit까지만)
	@Query(value = """
		    SELECT COUNT(*) FROM (
		        SELECT la.id
		        FROM lesson_applications la
		        WHERE la.lesson_id = :lessonId
		        LIMIT :limit
		    ) t
		""", nativeQuery = true)
	int countAllByLesson(@Param("lessonId") Long lessonId, @Param("limit") int limit);

	// 특정 상태 신청자 조회
	@Query(value = """
		    SELECT la.*
		    FROM lesson_applications la
		    LEFT JOIN user u ON la.user_id = u.id
		    LEFT JOIN profile p ON u.id = p.user_id
		    WHERE la.lesson_id = :lessonId
		      AND la.status = :status
		    ORDER BY la.created_at DESC
		    LIMIT :limit OFFSET :offset
		""", nativeQuery = true)
	List<LessonApplication> findAllByLessonAndStatus(@Param("lessonId") Long lessonId, @Param("status") String status,
		@Param("offset") int offset, @Param("limit") int limit);

	// 특정 상태 신청자 count
	@Query(value = """
		    SELECT COUNT(*) FROM (
		        SELECT la.id
		        FROM lesson_applications la
		        WHERE la.lesson_id = :lessonId
		          AND la.status = :status
		        LIMIT :limit
		    ) t
		""", nativeQuery = true)
	int countAllByLessonAndStatus(@Param("lessonId") Long lessonId, @Param("status") String status,
		@Param("limit") int limit);

	// 승인된 참가자 목록 조회
	@Query(value = """
		    SELECT la.*
		    FROM lesson_applications la
		    LEFT JOIN user u ON la.user_id = u.id
		    LEFT JOIN profile p ON u.id = p.user_id
		    WHERE la.lesson_id = :lessonId
		      AND la.status = 'APPROVED'
		    ORDER BY la.created_at ASC
		    LIMIT :limit OFFSET :offset
		""", nativeQuery = true)
	List<LessonApplication> findApprovedParticipants(@Param("lessonId") Long lessonId, @Param("offset") int offset,
		@Param("limit") int limit);

	// 승인된 참가자 count (countLimit까지만)
	@Query(value = """
		    SELECT COUNT(*) FROM (
		        SELECT la.id
		        FROM lesson_applications la
		        WHERE la.lesson_id = :lessonId
		          AND la.status = 'APPROVED'
		        LIMIT :limit
		    ) t
		""", nativeQuery = true)
	int countApprovedParticipants(@Param("lessonId") Long lessonId, @Param("limit") int limit);
}
