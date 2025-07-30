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
		    SELECT la.id
		    FROM lesson_applications la
		    WHERE la.user_id = :userId
		      AND (:status IS NULL OR la.status = :status)
		    ORDER BY la.created_at DESC
		    limit :limit OFFSET :offset
		""", nativeQuery = true)
	List<Long> findIdsByUserAndStatus(@Param("userId") Long userId,
		@Param("status") String status,
		@Param("offset") int offset,
		@Param("limit") int limit);

	@Query("""
		    SELECT la FROM LessonApplication la
		    JOIN FETCH la.lesson l
		    JOIN FETCH la.user u
		    JOIN FETCH u.profile p
          	LEFT JOIN FETCH u.profileMetadata pm
		    WHERE la.id IN :ids
		    ORDER BY la.createdAt DESC
		""")
	List<LessonApplication> findAllWithFetchJoin(@Param("ids") List<Long> ids);

	@Query(value = """
		   SELECT count(*)
		   FROM (
		       SELECT la.id
		       FROM lesson_applications la
		       WHERE la.user_id = :userId
		         AND (:status IS NULL OR la.status = :status)
		       limit :limit
		   ) t
		""", nativeQuery = true)
	int countByUserAndStatus(@Param("userId") Long userId, @Param("status") String status, @Param("limit") int limit);

	// 모든 상태 신청자 조회
	@Query(value = """
		    SELECT la.id
		    FROM lesson_applications la
		    WHERE la.lesson_id = :lessonId
		    ORDER BY la.created_at DESC
		    LIMIT :limit OFFSET :offset
		""", nativeQuery = true)
	List<Long> findIdsByLesson(@Param("lessonId") Long lessonId,
		@Param("offset") int offset,
		@Param("limit") int limit);

	@Query("""
		    SELECT DISTINCT la
		    FROM LessonApplication la
		    JOIN FETCH la.user u
		    JOIN FETCH la.lesson l
		    LEFT JOIN FETCH u.profile p
          	LEFT JOIN FETCH u.profileMetadata pm
		    WHERE la.id IN :ids
		    ORDER BY la.createdAt DESC
		""")
	List<LessonApplication> findAllWithUserProfileLesson(@Param("ids") List<Long> ids);

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
		    SELECT la.id
		    FROM lesson_applications la
		    WHERE la.lesson_id = :lessonId
		      AND la.status = :status
		    ORDER BY la.created_at DESC
		    LIMIT :limit OFFSET :offset
		""", nativeQuery = true)
	List<Long> findIdsByLessonAndStatus(@Param("lessonId") Long lessonId, @Param("status") String status,
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
}
