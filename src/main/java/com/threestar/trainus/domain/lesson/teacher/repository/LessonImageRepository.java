package com.threestar.trainus.domain.lesson.teacher.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonImage;

public interface LessonImageRepository extends JpaRepository<LessonImage, Long> {

	List<LessonImage> findByLesson(Lesson lesson);

	List<LessonImage> findAllByLessonId(Long id);
}

