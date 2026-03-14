package com.threestar.trainus.domain.lesson.student.service;

import com.threestar.trainus.domain.lesson.student.dto.LessonApplicationResponseDto;
import com.threestar.trainus.global.annotation.DistributedLock;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentLessonFacade {

	private final StudentLessonService studentLessonService;

	@DistributedLock(key = "'lesson_apply:' + #lessonId")
	public LessonApplicationResponseDto applyToLessonWithDistributedLock(Long lessonId, Long userId) {
		return studentLessonService.applyToLessonWithDistributedLock(lessonId, userId);
	}
}
