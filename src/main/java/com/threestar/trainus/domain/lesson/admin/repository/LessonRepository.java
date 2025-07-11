package com.threestar.trainus.domain.lesson.admin.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.lesson.admin.entity.LessonStatus;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
	//삭제되지 않은 레슨만 조회
	Optional<Lesson> findByIdAndDeletedAtIsNull(Long lessonId);

	// 중복 레슨 검증(같은 강사가 같은 이름과 시작시간으로 레슨 생성했는지 체크)
	@Query("SELECT COUNT(l) > 0 FROM Lesson l WHERE " + "l.lessonLeader = :lessonLeader AND "
		+ "l.lessonName = :lessonName AND " + "l.startAt = :startAt AND " + "l.deletedAt IS NULL")
	boolean existsDuplicateLesson(@Param("lessonLeader") Long lessonLeader,
		@Param("lessonName") String lessonName,
		@Param("startAt") LocalDateTime startAt);

	// 시간 겹침 검증(같은 강사가 동일 시간대에 다른 레슨이 있는지 체크)
	@Query(
		"SELECT COUNT(l) > 0 FROM Lesson l WHERE " + "l.lessonLeader = :lessonLeader AND " + "l.deletedAt IS NULL AND "
			+ "(l.startAt < :endAt AND l.endAt > :startAt)")
	boolean hasTimeConflictLesson(@Param("lessonLeader") Long lessonLeader,
		@Param("startAt") LocalDateTime startAt,
		@Param("endAt") LocalDateTime endAt);

	// 강사가 개설한 레슨 목록 조회 (페이징)
	Page<Lesson> findByLessonLeaderAndDeletedAtIsNull(Long lessonLeader, Pageable pageable);

	// 강사가 개설한 레슨 목록 조회 (페이징+필터링)
	Page<Lesson> findByLessonLeaderAndStatusAndDeletedAtIsNull(Long lessonLeader, LessonStatus status,
		Pageable pageable);

}
