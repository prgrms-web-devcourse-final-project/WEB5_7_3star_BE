package com.threestar.trainus.domain.lesson.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.lesson.admin.entity.LessonImage;

@Repository
public interface LessonImageRepository extends JpaRepository<LessonImage, Long> {

	List<LessonImage> findByLesson(Lesson lesson);
}

