package com.threestar.trainus.domain.lesson.student.dto;

public record PaginationDto(
	int currentPage,
	int totalPages,
	long totalCount,
	int limit
) {}