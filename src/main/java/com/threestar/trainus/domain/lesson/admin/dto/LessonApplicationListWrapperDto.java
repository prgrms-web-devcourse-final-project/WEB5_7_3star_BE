package com.threestar.trainus.domain.lesson.admin.dto;

import java.util.List;

public record LessonApplicationListWrapperDto(
	List<LessonApplicationResponseDto> lessonApplications
) {
}
