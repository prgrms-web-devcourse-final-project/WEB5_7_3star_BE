package com.threestar.trainus.domain.lesson.admin.mapper;

import java.util.List;

import com.threestar.trainus.domain.lesson.admin.dto.CreatedLessonDto;
import com.threestar.trainus.domain.lesson.admin.dto.CreatedLessonListResponseDto;
import com.threestar.trainus.domain.lesson.admin.entity.Lesson;

public class CreatedLessonMapper {

	// Lesson 엔티티를 CreatedLessonDto로 변환
	public static CreatedLessonDto toCreatedLessonDto(Lesson lesson) {
		return CreatedLessonDto.builder()
			.id(lesson.getId())
			.lessonName(lesson.getLessonName())
			.maxParticipants(lesson.getMaxParticipants())
			.currentParticipants(lesson.getParticipantCount())
			.price(lesson.getPrice())
			.status(lesson.getStatus().name())
			.startAt(lesson.getStartAt())
			.endAt(lesson.getEndAt())
			.openRun(lesson.getOpenRun())
			.addressDetail(lesson.getAddressDetail())
			.build();
	}

	// 개설한 레슨 목록과 총 레슨의 수를 응답 DTO로 변환
	public static CreatedLessonListResponseDto toCreatedLessonListResponseDto(
		List<Lesson> lessons, Long totalCount) {

		// 각 레슨을 DTO로 변환
		List<CreatedLessonDto> lessonDtos = lessons.stream()
			.map(CreatedLessonMapper::toCreatedLessonDto)
			.toList();

		// 응답 DTO 생성
		return CreatedLessonListResponseDto.builder()
			.lessons(lessonDtos)
			.count(totalCount)
			.build();
	}
}
