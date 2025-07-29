package com.threestar.trainus.domain.lesson.student.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LessonSortType {
	LATEST("createdAt"),
	OLDEST("createdAt"),
	PRICE_HIGH("price"),
	PRICE_LOW("price");
	private final String property;
}
