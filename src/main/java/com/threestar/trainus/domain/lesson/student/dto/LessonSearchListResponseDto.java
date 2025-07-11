package com.threestar.trainus.domain.lesson.student.dto;

import java.util.List;

public record LessonSearchListResponseDto(
	List<LessonSearchResponseDto> lessons,
	PaginationDto pagination
) {}

