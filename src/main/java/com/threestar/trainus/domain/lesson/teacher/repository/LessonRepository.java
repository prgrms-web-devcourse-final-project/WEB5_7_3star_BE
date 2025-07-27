package com.threestar.trainus.domain.lesson.teacher.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonStatus;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
	//삭제되지 않은 레슨만 조회
	Optional<Lesson> findByIdAndDeletedAtIsNull(Long lessonId);

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

	// 강사가 개설한 레슨 전체 목록 상태에 따라 필터링해서 조회(모집중인것만...이런식으로)
	Page<Lesson> findByLessonLeaderAndDeletedAtIsNull(Long lessonLeader, Pageable pageable);

	// 강사가 개설한 레슨 전체 목록 조회
	Page<Lesson> findByLessonLeaderAndStatusAndDeletedAtIsNull(Long lessonLeader, LessonStatus status,
		Pageable pageable);

	// 레슨 검색
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
	Page<Lesson> findBySearchConditions(
		@Param("category") Category category,
		@Param("city") String city,
		@Param("district") String district,
		@Param("dong") String dong,
		@Param("search") String search,
		Pageable pageable
	);

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
}
