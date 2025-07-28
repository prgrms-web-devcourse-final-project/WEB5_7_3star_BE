package com.threestar.trainus.domain.lesson.teacher.dto;

import java.util.List;

public record CreatedLessonListWrapperDto(
	List<CreatedLessonDto> lessons
) {
}
