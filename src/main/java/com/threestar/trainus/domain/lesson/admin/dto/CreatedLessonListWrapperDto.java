package com.threestar.trainus.domain.lesson.admin.dto;

import java.util.List;

public record CreatedLessonListWrapperDto(
	List<CreatedLessonDto> lessons
) {
}
