package com.threestar.trainus.domain.lesson.student.mapper;

import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.lesson.student.dto.LessonSimpleResponseDto;

public class LessonSimpleMapper {

	public static LessonSimpleResponseDto toLessonSimpleDto(Lesson lesson) {
		return LessonSimpleResponseDto.builder()
			.lessonId(lesson.getId())
			.lessonName(lesson.getLessonName())
			.startAt(lesson.getStartAt())
			.endAt(lesson.getEndAt())
			.price(Long.valueOf(lesson.getPrice()))
			.addressDetail(lesson.getAddressDetail())
			.build();
	}
}
