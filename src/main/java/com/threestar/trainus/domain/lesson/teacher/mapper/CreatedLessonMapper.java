package com.threestar.trainus.domain.lesson.teacher.mapper;

import java.util.List;

import com.threestar.trainus.domain.lesson.teacher.dto.CreatedLessonDto;
import com.threestar.trainus.domain.lesson.teacher.dto.CreatedLessonListResponseDto;
import com.threestar.trainus.domain.lesson.teacher.dto.CreatedLessonListWrapperDto;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;

public class CreatedLessonMapper {

	// Lesson 엔티티를 CreatedLessonDto로 변환
	public static CreatedLessonDto toCreatedLessonDto(Lesson lesson) {
		Double latitude = lesson.getLocationPoint() != null ? lesson.getLocationPoint().getY() : null;
		Double longitude = lesson.getLocationPoint() != null ? lesson.getLocationPoint().getX() : null;

		return CreatedLessonDto.builder()
			.id(lesson.getId())
			.lessonName(lesson.getLessonName())
			.maxParticipants(lesson.getMaxParticipants())
			.currentParticipants(lesson.getParticipantCount())
			.price(lesson.getPrice())
			.status(lesson.getStatus())
			.startAt(lesson.getStartAt())
			.endAt(lesson.getEndAt())
			.openTime(lesson.getOpenTime())
			.openRun(lesson.getOpenRun())
			.address(lesson.getAddress())
			.addressDetail(lesson.getAddressDetail())
			.latitude(latitude)
			.longitude(longitude)
			.build();
	}

	// 개설한 레슨 목록과 총 레슨의 수를 응답 DTO로 변환
	public static CreatedLessonListResponseDto toCreatedLessonListResponseDto(
		List<Lesson> lessons, int totalCount) {

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

	public static CreatedLessonListWrapperDto toCreatedLessonListWrapperDto(
		CreatedLessonListResponseDto responseDto) {
		return new CreatedLessonListWrapperDto(responseDto.lessons());
	}
}
