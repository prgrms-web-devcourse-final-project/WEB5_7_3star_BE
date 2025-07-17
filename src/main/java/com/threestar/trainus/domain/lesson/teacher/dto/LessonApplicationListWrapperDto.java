package com.threestar.trainus.domain.lesson.teacher.dto;

import java.util.List;

public record LessonApplicationListWrapperDto(
	List<LessonApplicationResponseDto> lessonApplications
) {
}
