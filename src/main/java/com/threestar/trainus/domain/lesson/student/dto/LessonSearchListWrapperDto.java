package com.threestar.trainus.domain.lesson.student.dto;

import java.util.List;

public record LessonSearchListWrapperDto(
	List<LessonSearchResponseDto> lessons
) {
}
