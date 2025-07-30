package com.threestar.trainus.domain.lesson.teacher.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonStatus;

import jakarta.persistence.LockModeType;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

	// 중복 레슨 검증(같은 강사가 같은 이름과 시작시간으로 레슨 생성했는지 체크)
	@Query("""
		SELECT COUNT(l) > 0 FROM Lesson l
		WHERE l.lessonLeader = :lessonLeader
		AND l.lessonName = :lessonName
		AND l.startAt = :startAt
		AND l.deletedAt IS NULL
		""")
	boolean existsDuplicateLesson(@Param("lessonLeader") Long lessonLeader,
		@Param("lessonName") String lessonName,
		@Param("startAt") LocalDateTime startAt);

	// 시간 겹침 검증(같은 강사가 동일 시간대에 다른 레슨이 있는지 체크) ->레슨생성시에 사용
	@Query("""
		SELECT COUNT(l) > 0 FROM Lesson l
		WHERE l.lessonLeader = :lessonLeader
		AND l.deletedAt IS NULL
		AND (l.startAt < :endAt AND l.endAt > :startAt)
		""")
	boolean hasTimeConflictLesson(@Param("lessonLeader") Long lessonLeader,
		@Param("startAt") LocalDateTime startAt,
		@Param("endAt") LocalDateTime endAt);

	//시간 겹침 검증 ->레슨 수정시 사용 (현재 수정중인 레슨 제외)
	@Query("""
		SELECT COUNT(l) > 0 FROM Lesson l
		WHERE l.lessonLeader = :lessonLeader
		AND l.deletedAt IS NULL
		AND l.id != :excludeLessonId
		AND (l.startAt < :endAt AND l.endAt > :startAt)
		""")
	boolean hasTimeConflictForUpdate(
		@Param("lessonLeader") Long lessonLeader,
		@Param("startAt") LocalDateTime startAt,
		@Param("endAt") LocalDateTime endAt,
		@Param("excludeLessonId") Long excludeLessonId
	);

	// 강사가 개설한 레슨 목록 조회 (전체 목록 - 탈퇴 검증용)
	List<Lesson> findByLessonLeaderAndDeletedAtIsNull(Long lessonLeader);

	// 시작할 레슨을 찾는 메서드
	// 모집중이거나 모집완료 상태일 때, 시작 시간이 도달한 레슨
	@Query("""
		SELECT l FROM Lesson l
		WHERE l.status IN (
			com.threestar.trainus.domain.lesson.teacher.entity.LessonStatus.RECRUITING,
			com.threestar.trainus.domain.lesson.teacher.entity.LessonStatus.RECRUITMENT_COMPLETED
		)
		AND l.startAt <= :now
		AND l.deletedAt IS NULL
		""")
	List<Lesson> findLessonsToStart(@Param("now") LocalDateTime now);

	//완료할 레슨을 찾는 메서드
	//현재는 진행중 -> 종료시간이 지나면 종료중으로 바뀔 레슨
	@Query("""
		SELECT l FROM Lesson l
		WHERE l.status = com.threestar.trainus.domain.lesson.teacher.entity.LessonStatus.IN_PROGRESS
		AND l.endAt <= :now
		AND l.deletedAt IS NULL
		""")
	List<Lesson> findLessonsToComplete(@Param("now") LocalDateTime now);

	//레슨ID로 레슨 조회 (비관적 락 적용)
	@Lock(LockModeType.PESSIMISTIC_WRITE) // 비관적 락 적용
	@Query("SELECT l FROM Lesson l WHERE l.id = :lessonId")
	Optional<Lesson> findByIdWithLock(@Param("lessonId") Long lessonId);

	@Query("""
		SELECT l FROM Lesson l
		WHERE l.city = :city
		AND l.district = :district
		AND l.dong = :dong
		AND (:ri IS NULL OR l.ri = :ri)
		AND (:category IS NULL OR l.category = :category)
		""")
	Page<Lesson> findByLocation(
		@Param("category") Category category,
		@Param("city") String city,
		@Param("district") String district,
		@Param("dong") String dong,
		@Param("ri") String ri,
		Pageable pageable
	);

	// 주소로만 검색
	// LIKE 검색
	@Query("""
		SELECT l FROM Lesson l
		WHERE
			(:category IS NULL OR l.category = :category)
			AND l.city = :city
			AND l.district = :district
			AND l.dong = :dong
			AND (
				:search IS NULL OR
				LOWER(l.lessonName) LIKE LOWER(CONCAT('%', :search, '%'))
			)
		""")
	Page<Lesson> findByLocationAndSearchWithLike(
		@Param("category") Category category,
		@Param("city") String city,
		@Param("district") String district,
		@Param("dong") String dong,
		@Param("ri") String ri,
		@Param("search") String search,
		Pageable pageable
	);

	// Full-Text 검색 최적화 (서브쿼리 JOIN 방식)
	@Query(
		value = """
				SELECT l.* FROM lessons l
				JOIN (
					SELECT id FROM lessons
					WHERE MATCH(lesson_name) AGAINST(:search IN BOOLEAN MODE)
				) AS ft ON l.id = ft.id
				WHERE l.city = :city AND l.district = :district AND l.dong = :dong AND (:ri IS NULL OR l.ri = :ri) AND (:category IS NULL OR l.category = :category)
				ORDER BY l.created_at DESC
			""",
		countQuery = """
				SELECT count(l.id) FROM lessons l
				JOIN (
					SELECT id FROM lessons
					WHERE MATCH(lesson_name) AGAINST(:search IN BOOLEAN MODE)
				) AS ft ON l.id = ft.id
				WHERE l.city = :city AND l.district = :district AND l.dong = :dong AND (:ri IS NULL OR l.ri = :ri) AND (:category IS NULL OR l.category = :category)
				ORDER BY l.created_at DESC
			""",
		nativeQuery = true
	)
	Page<Lesson> findByLocationAndFullTextSearchOptimized(
		@Param("category") Category category,
		@Param("city") String city,
		@Param("district") String district,
		@Param("dong") String dong,
		@Param("ri") String ri,
		@Param("search") String search,
		Pageable pageable
	);

	// 목록 조회
	@Query(value = """
		    SELECT *
		    FROM lessons l
		    WHERE l.lesson_leader = :userId
		      AND (:status IS NULL OR l.status = :status)
		      AND l.deleted_at IS NULL
		    ORDER BY l.created_at DESC
		    LIMIT :limit OFFSET :offset
		""", nativeQuery = true)
	List<Lesson> findCreatedLessonsByUser(
		@Param("userId") Long userId,
		@Param("status") String status,
		@Param("limit") int limit,
		@Param("offset") int offset
	);

	// totalCount (상태 포함)
	@Query(value = """
		    SELECT COUNT(*) FROM (
		        SELECT l.id
		        FROM lessons l
		        WHERE l.lesson_leader = :userId
		          AND l.status = :status
		          AND l.deleted_at IS NULL
		        LIMIT :limit
		    ) t
		""", nativeQuery = true)
	int countCreatedLessonsByStatus(
		@Param("userId") Long userId,
		@Param("status") LessonStatus status,
		@Param("limit") int limit
	);

	// totalCount (모든 상태)
	@Query(value = """
		    SELECT COUNT(*) FROM (
		        SELECT l.id
		        FROM lessons l
		        WHERE l.lesson_leader = :userId
		          AND l.deleted_at IS NULL
		        LIMIT :limit
		    ) t
		""", nativeQuery = true)
	int countCreatedLessons(
		@Param("userId") Long userId,
		@Param("limit") int limit
	);

	@Query(value = """
		    SELECT l.* 
		    FROM lessons l
		    WHERE (:category IS NULL OR l.category = :category)
		      AND (:city IS NULL OR l.city = :city)
		      AND (:district IS NULL OR l.district = :district)
		      AND (:dong IS NULL OR l.dong = :dong)
		      AND (:ri IS NULL OR l.ri = :ri)
		      AND MATCH(l.lesson_name, l.description) AGAINST(:search IN BOOLEAN MODE)
		    ORDER BY 
		      CASE WHEN :sort = 'LATEST' THEN l.created_at END DESC,
		      CASE WHEN :sort = 'OLDEST' THEN l.created_at END ASC,
		      CASE WHEN :sort = 'PRICE_HIGH' THEN l.price END DESC,
		      CASE WHEN :sort = 'PRICE_LOW' THEN l.price END ASC,
		      l.created_at DESC
		    LIMIT :limit OFFSET :offset
		""", nativeQuery = true)
	List<Lesson> findLessonsWithFullText(
		@Param("category") String category,
		@Param("city") String city,
		@Param("district") String district,
		@Param("dong") String dong,
		@Param("ri") String ri,
		@Param("search") String search,
		@Param("sort") String sort,
		@Param("offset") int offset,
		@Param("limit") int limit
	);

	@Query(value = """
		    SELECT COUNT(*) FROM (
		        SELECT l.id
		        FROM lessons l
		        WHERE (:category IS NULL OR l.category = :category)
		          AND (:city IS NULL OR l.city = :city)
		          AND (:district IS NULL OR l.district = :district)
		          AND (:dong IS NULL OR l.dong = :dong)
		          AND (:ri IS NULL OR l.ri = :ri)
		          AND MATCH(l.lesson_name, l.description) AGAINST(:search IN BOOLEAN MODE)
		        LIMIT :limit
		    ) t
		""", nativeQuery = true)
	int countLessonsWithFullText(
		@Param("category") String category,
		@Param("city") String city,
		@Param("district") String district,
		@Param("dong") String dong,
		@Param("ri") String ri,
		@Param("search") String search,
		@Param("limit") int limit
	);

	@Query(value = """
		    SELECT l.* 
		    FROM lessons l
		    WHERE (:category IS NULL OR l.category = :category)
		      AND (:city IS NULL OR l.city = :city)
		      AND (:district IS NULL OR l.district = :district)
		      AND (:dong IS NULL OR l.dong = :dong)
		      AND (:ri IS NULL OR l.ri = :ri)
		    ORDER BY 
		      CASE WHEN :sort = 'LATEST' THEN l.created_at END DESC,
		      CASE WHEN :sort = 'OLDEST' THEN l.created_at END ASC,
		      CASE WHEN :sort = 'PRICE_HIGH' THEN l.price END DESC,
		      CASE WHEN :sort = 'PRICE_LOW' THEN l.price END ASC,
		      l.created_at DESC
		    LIMIT :limit OFFSET :offset
		""", nativeQuery = true)
	List<Lesson> findLessonsWithoutFullText(
		@Param("category") String category,
		@Param("city") String city,
		@Param("district") String district,
		@Param("dong") String dong,
		@Param("ri") String ri,
		@Param("sort") String sort,
		@Param("offset") int offset,
		@Param("limit") int limit
	);

	@Query(value = """
		    SELECT COUNT(*) FROM (
		        SELECT l.id
		        FROM lessons l
		        WHERE (:category IS NULL OR l.category = :category)
		          AND (:city IS NULL OR l.city = :city)
		          AND (:district IS NULL OR l.district = :district)
		          AND (:dong IS NULL OR l.dong = :dong)
		          AND (:ri IS NULL OR l.ri = :ri)
		        LIMIT :limit
		    ) t
		""", nativeQuery = true)
	int countLessonsWithoutFullText(
		@Param("category") String category,
		@Param("city") String city,
		@Param("district") String district,
		@Param("dong") String dong,
		@Param("ri") String ri,
		@Param("limit") int limit
	);

	// 상태 필터 없는 전체 조회
	@Query(value = """
		    SELECT *
		    FROM lessons l
		    WHERE l.lesson_leader = :userId
		      AND l.deleted_at IS NULL
		    ORDER BY l.created_at DESC
		    LIMIT :limit OFFSET :offset
		""", nativeQuery = true)
	List<Lesson> findCreatedLessons(
		@Param("userId") Long userId,
		@Param("offset") int offset,
		@Param("limit") int limit
	);

	// // 상태가 있는 경우
	@Query(value = """
		    SELECT *
		    FROM lessons l
		    WHERE l.lesson_leader = :userId
		      AND l.status = :status
		      AND l.deleted_at IS NULL
		    ORDER BY l.created_at DESC
		    LIMIT :limit OFFSET :offset
		""", nativeQuery = true)
	List<Lesson> findCreatedLessonsByStatus(
		@Param("userId") Long userId,
		@Param("status") LessonStatus status,
		@Param("offset") int offset,
		@Param("limit") int limit
	);

}
