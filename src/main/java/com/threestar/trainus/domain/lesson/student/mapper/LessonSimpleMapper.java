package com.threestar.trainus.domain.lesson.student.mapper;

import com.threestar.trainus.domain.lesson.student.dto.LessonSimpleResponseDto;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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
