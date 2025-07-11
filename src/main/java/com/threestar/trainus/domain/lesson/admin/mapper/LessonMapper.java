package com.threestar.trainus.domain.lesson.admin.mapper;

import java.util.List;

import com.threestar.trainus.domain.lesson.admin.dto.LessonCreateRequestDto;
import com.threestar.trainus.domain.lesson.admin.dto.LessonResponseDto;
import com.threestar.trainus.domain.lesson.admin.entity.Lesson;
import com.threestar.trainus.domain.lesson.admin.entity.LessonImage;
import com.threestar.trainus.domain.lesson.student.dto.LessonDetailResponseDto;
import com.threestar.trainus.domain.profile.entity.Profile;
import com.threestar.trainus.domain.user.entity.User;

public class LessonMapper {

	//레슨 생성 요청 DTO를 레슨 엔티티로 변환
	public static Lesson toEntity(LessonCreateRequestDto requestDto, User user) {
		return Lesson.builder()
			.lessonLeader(user.getId())
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
	public static LessonResponseDto toResponseDto(Lesson lesson, List<LessonImage> lessonImages) {
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

	// 상세 조회 DTO 변환
	public static LessonDetailResponseDto toLessonDetailDto(
		Lesson lesson,
		User leader,
		Profile profile,
		int reviewCount,
		float rating,
		List<String> lessonImages
	) {
		return LessonDetailResponseDto.builder()
			.id(lesson.getId())
			.lessonName(lesson.getLessonName())
			.description(lesson.getDescription())
			.lessonLeader(leader.getId())
			.lessonLeaderName(leader.getNickname())
			.profileIntro(profile.getIntro())
			.profileImage(profile.getProfileImage())
			.reviewCount(reviewCount)
			.rating(rating)
			.category(lesson.getCategory().name())
			.price(lesson.getPrice())
			.maxParticipants(lesson.getMaxParticipants())
			.currentParticipants(lesson.getParticipantCount())
			.status(lesson.getStatus().name())
			.startAt(lesson.getStartAt())
			.endAt(lesson.getEndAt())
			.openTime(lesson.getOpenTime())
			.openRun(lesson.getOpenRun())
			.city(lesson.getCity())
			.district(lesson.getDistrict())
			.dong(lesson.getDong())
			.addressDetail(lesson.getAddressDetail())
			.createdAt(lesson.getCreatedAt())
			.updatedAt(lesson.getUpdatedAt())
			.lessonImages(lessonImages)
			.build();
	}
}
