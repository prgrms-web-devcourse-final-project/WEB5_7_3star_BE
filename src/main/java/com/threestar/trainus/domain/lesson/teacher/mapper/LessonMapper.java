package com.threestar.trainus.domain.lesson.teacher.mapper;

import java.util.List;

import com.threestar.trainus.domain.lesson.student.dto.LessonDetailResponseDto;
import com.threestar.trainus.domain.lesson.teacher.dto.LessonCreateRequestDto;
import com.threestar.trainus.domain.lesson.teacher.dto.LessonResponseDto;
import com.threestar.trainus.domain.lesson.teacher.dto.LessonUpdateResponseDto;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonImage;
import com.threestar.trainus.domain.profile.entity.Profile;
import com.threestar.trainus.domain.user.entity.User;

import org.locationtech.jts.geom.Point;

public class LessonMapper {

	//레슨 생성 요청 DTO를 레슨 엔티티로 변환
	public static Lesson toEntity(LessonCreateRequestDto requestDto, User user, Point locationPoint) {
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
			.ri(requestDto.ri())
			.address(requestDto.address())
			.addressDetail(requestDto.addressDetail())
			.locationPoint(locationPoint)
			.build();
	}

	//레슨 엔티티와 이미지 목록을 레슨 응답 DTO로 변환
	public static LessonResponseDto toResponseDto(Lesson lesson, List<LessonImage> lessonImages) {
		// 이미지 엔티티 목록에서 URL만 추출
		List<String> imageUrls = lessonImages.stream().map(LessonImage::getImageUrl).toList();

		Double latitude = lesson.getLocationPoint() != null ? lesson.getLocationPoint().getY() : null;
		Double longitude = lesson.getLocationPoint() != null ? lesson.getLocationPoint().getX() : null;

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
			.ri(lesson.getRi())
			.address(lesson.getAddress())
			.addressDetail(lesson.getAddressDetail())
			.latitude(latitude)
			.longitude(longitude)
			.status(lesson.getStatus())
			.createdAt(lesson.getCreatedAt())
			.lessonImages(imageUrls) // 이미지 URL 목록
			.build();
	}

	// 상세 조회 DTO 변환
	public static LessonDetailResponseDto toLessonDetailDto(Lesson lesson, User leader, Profile profile,
		int reviewCount, double rating, List<String> lessonImages) {
		Double latitude = lesson.getLocationPoint() != null ? lesson.getLocationPoint().getY() : null;
		Double longitude = lesson.getLocationPoint() != null ? lesson.getLocationPoint().getX() : null;

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
			.category(lesson.getCategory())
			.price(lesson.getPrice())
			.maxParticipants(lesson.getMaxParticipants())
			.currentParticipants(lesson.getParticipantCount())
			.status(lesson.getStatus())
			.startAt(lesson.getStartAt())
			.endAt(lesson.getEndAt())
			.openTime(lesson.getOpenTime())
			.openRun(lesson.getOpenRun())
			.city(lesson.getCity())
			.district(lesson.getDistrict())
			.dong(lesson.getDong())
			.ri(lesson.getRi())
			.address(lesson.getAddress())
			.addressDetail(lesson.getAddressDetail())
			.latitude(latitude)
			.longitude(longitude)
			.createdAt(lesson.getCreatedAt())
			.updatedAt(lesson.getUpdatedAt())
			.lessonImages(lessonImages)
			.build();
	}

	//레슨 수정
	public static LessonUpdateResponseDto toUpdateResponseDto(Lesson lesson, List<LessonImage> lessonImages) {
		// 이미지 엔티티 목록에서 URL만 추출
		List<String> imageUrls = lessonImages.stream().map(LessonImage::getImageUrl).toList();

		Double latitude = lesson.getLocationPoint() != null ? lesson.getLocationPoint().getY() : null;
		Double longitude = lesson.getLocationPoint() != null ? lesson.getLocationPoint().getX() : null;

		return LessonUpdateResponseDto.builder()
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
			.ri(lesson.getRi())
			.address(lesson.getAddress())
			.addressDetail(lesson.getAddressDetail())
			.latitude(latitude)
			.longitude(longitude)
			.status(lesson.getStatus())
			.createdAt(lesson.getCreatedAt())
			.updatedAt(lesson.getUpdatedAt())
			.lessonImages(imageUrls)
			.build();
	}
}
