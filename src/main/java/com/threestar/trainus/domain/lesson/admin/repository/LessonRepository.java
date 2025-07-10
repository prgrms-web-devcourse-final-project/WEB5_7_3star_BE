package com.threestar.trainus.domain.lesson.admin.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.threestar.trainus.domain.lesson.admin.entity.Lesson;

import io.lettuce.core.dynamic.annotation.Param;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
	// 강사가 개설한 레슨 목록 조회용
	List<Lesson> findByLessonLeaderAndDeletedAtIsNull(Long lessonLeader);

	//삭제되지 않은 레슨만 조회
	Optional<Lesson> findByIdAndDeletedAtIsNull(Long lessonId);

	// 중복 레슨 검증(동일한 강사가 같은 이름+시간으로 레슨 생성 차단)
	@Query("SELECT COUNT(l) > 0 FROM Lesson l WHERE " + "l.lessonLeader = :lessonLeader AND "
		+ "l.lessonName = :lessonName AND " + "l.startAt = :startAt AND " + "l.deletedAt IS NULL")
	boolean existsDuplicateLesson(@Param("lessonLeader") Long lessonLeader,
		@Param("lessonName") String lessonName,
		@Param("startAt") LocalDateTime startAt);

	// 시간 겹침 검증(같은 강사가 동일 시간대에 여러 레슨 생성 차단)
	@Query(
		"SELECT COUNT(l) > 0 FROM Lesson l WHERE " + "l.lessonLeader = :lessonLeader AND " + "l.deletedAt IS NULL AND "
			+ "(l.startAt < :endAt AND l.endAt > :startAt)")
	boolean hasTimeConflictLesson(@Param("lessonLeader") Long lessonLeader,
		@Param("startAt") LocalDateTime startAt,
		@Param("endAt") LocalDateTime endAt);
}
