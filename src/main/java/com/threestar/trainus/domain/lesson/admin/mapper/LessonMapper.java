package com.threestar.trainus.domain.lesson.admin.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.threestar.trainus.domain.lesson.admin.dto.LessonCreateRequestDto;
import com.threestar.trainus.domain.lesson.admin.dto.LessonResponseDto;
import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.lesson.admin.entity.LessonImage;

@Component
public class LessonMapper {

	public Lesson toEntity(LessonCreateRequestDto requestDto, Long userId) {
		return Lesson.builder()
			.lessonLeader(userId)
			.lessonName(requestDto.getLessonName())
			.description(requestDto.getDescription())
			.maxParticipants(requestDto.getMaxParticipants())
			.startAt(requestDto.getStartAt())
			.endAt(requestDto.getEndAt())
			.price(requestDto.getPrice())
			.category(requestDto.getCategory())
			.openTime(requestDto.getOpenTime())
			.openRun(requestDto.getOpenRun())
			.city(requestDto.getCity())
			.district(requestDto.getDistrict())
			.dong(requestDto.getDong())
			.addressDetail(requestDto.getAddressDetail())
			.build();
	}

	public LessonResponseDto toResponseDto(Lesson lesson, List<LessonImage> lessonImages) {
		List<String> imageUrls = lessonImages.stream()
			.map(LessonImage::getImageUrl)
			.toList();

		return LessonResponseDto.builder()
			.id(lesson.getId())
			.lessonName(lesson.getLessonName())
			.description(lesson.getDescription())
			.lessonLeader(lesson.getLessonLeader())
			.category(lesson.getCategory())
			.price(lesson.getPrice())
			.maxParticipants(lesson.getMaxParticipants())
			.startAt(lesson.getStartAt())
			.endAt(lesson.getEndAt())
			.openTime(lesson.getOpenTime())
			.openRun(lesson.getOpenRun())
			.city(lesson.getCity())
			.district(lesson.getDistrict())
			.dong(lesson.getDong())
			.addressDetail(lesson.getAddressDetail())
			.status(lesson.getStatus().name()) //enum이름만 반환하도록
			.createdAt(lesson.getCreatedAt())
			.lessonImages(imageUrls)
			.build();
	}
}
