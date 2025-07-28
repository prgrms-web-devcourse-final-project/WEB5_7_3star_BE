package com.threestar.trainus.domain.lesson.student.mapper;

import java.time.LocalDateTime;

import com.threestar.trainus.domain.lesson.student.dto.LessonApplicationResponseDto;
import com.threestar.trainus.domain.lesson.teacher.entity.ApplicationStatus;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LessonApplyMapper {
	public static LessonApplicationResponseDto toLessonApplicationResponseDto(
		Long lessonId,
		Long userId,
		ApplicationStatus status, // APPROVED, PENDING
		LocalDateTime appliedAt
	) {
		return LessonApplicationResponseDto.builder()
			.lessonId(lessonId)
			.userId(userId)
			.status(status)
			.appliedAt(appliedAt)
			.build();
	}
}
