package com.threestar.trainus.domain.lesson.admin.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.threestar.trainus.domain.lesson.admin.dto.LessonCreateRequestDto;
import com.threestar.trainus.domain.lesson.admin.dto.LessonResponseDto;
import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.lesson.admin.entity.LessonImage;

@Component
public class LessonMapper {

	//레슨 생성 요청 DTO를 레슨 엔티티로 변환
	public Lesson toEntity(LessonCreateRequestDto requestDto, Long userId) {
		return Lesson.builder()
			.lessonLeader(userId)  //강사 id설정
			.lessonName(requestDto.lessonName())
			.description(requestDto.description())
			.maxParticipants(requestDto.maxParticipants())
			.startAt(requestDto.startAt())
			.endAt(requestDto.endAt())
			.price(requestDto.price())
			.category(requestDto.category())
			.openTime(requestDto.openTime())
			.openRun(requestDto.openRun())
			.city(requestDto.city())
			.district(requestDto.district())
			.dong(requestDto.dong())
			.addressDetail(requestDto.addressDetail())
			.build();
	}

	//레슨 엔티티와 이미지 목록을 레슨 응답 DTO로 변환
	public LessonResponseDto toResponseDto(Lesson lesson, List<LessonImage> lessonImages) {
		// 이미지 엔티티 목록에서 URL만 추출
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
			.status(lesson.getStatus().name()) // enum이름 반환
			.createdAt(lesson.getCreatedAt())
			.lessonImages(imageUrls) // 이미지 URL 목록
			.build();
	}
}
