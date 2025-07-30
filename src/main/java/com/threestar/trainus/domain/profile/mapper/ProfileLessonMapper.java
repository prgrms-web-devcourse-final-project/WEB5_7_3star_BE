package com.threestar.trainus.domain.profile.mapper;

import java.util.List;

import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.profile.dto.ProfileCreatedLessonDto;
import com.threestar.trainus.domain.profile.dto.ProfileCreatedLessonListResponseDto;
import com.threestar.trainus.domain.profile.dto.ProfileCreatedLessonListWrapperDto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileLessonMapper {

	// Lesson 엔티티를 ProfileCreatedLessonDto로 변환
	public static ProfileCreatedLessonDto toProfileCreatedLessonDto(Lesson lesson) {
		return ProfileCreatedLessonDto.builder()
			.id(lesson.getId())
			.lessonName(lesson.getLessonName())
			.maxParticipants(lesson.getMaxParticipants())
			.currentParticipants(lesson.getParticipantCount())
			.price(lesson.getPrice())
			.status(lesson.getStatus())
			.startAt(lesson.getStartAt())
			.endAt(lesson.getEndAt())
			.openRun(lesson.getOpenRun())
			.addressDetail(lesson.getAddressDetail())
			.build();
	}

	// 개설한 레슨 목록과 총 레슨의 수를 응답 DTO로 변환
	public static ProfileCreatedLessonListResponseDto toProfileCreatedLessonListResponseDto(
		List<Lesson> lessons, int totalCount) {

		// 각 레슨을 DTO로 변환
		List<ProfileCreatedLessonDto> lessonDtos = lessons.stream()
			.map(ProfileLessonMapper::toProfileCreatedLessonDto)
			.toList();
		return ProfileCreatedLessonListResponseDto.builder()
			.lessons(lessonDtos)
			.count(totalCount)
			.build();
	}

	public static ProfileCreatedLessonListWrapperDto toProfileCreatedLessonListWrapperDto(
		ProfileCreatedLessonListResponseDto responseDto) {
		return new ProfileCreatedLessonListWrapperDto(responseDto.lessons());
	}
}
