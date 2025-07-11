package com.threestar.trainus.domain.lesson.admin.mapper;

import java.util.List;

import com.threestar.trainus.domain.lesson.admin.dto.LessonApplicationListResponseDto;
import com.threestar.trainus.domain.lesson.admin.dto.LessonApplicationResponseDto;
import com.threestar.trainus.domain.lesson.admin.entity.LessonApplication;
import com.threestar.trainus.domain.profile.dto.ProfileResponseDto;

public class LessonApplicationMapper {

	//LessonApplication 엔티티를 LessonApplicationResponseDto로 변환
	public static LessonApplicationResponseDto toResponseDto(LessonApplication application) {
		// User 엔티티에서 정보 가져오기
		ProfileResponseDto userDto = new ProfileResponseDto(
			application.getUser().getId(),
			application.getUser().getNickname(),
			application.getUser().getProfile().getProfileImage(),
			application.getUser().getProfile().getIntro()
		);

		return LessonApplicationResponseDto.builder()
			.lessonApplicationId(application.getId())
			.user(userDto)
			.status(application.getStatus().name())
			.appliedAt(application.getCreatedAt())
			.build();
	}

	//신청자 목록과 전체 개수를 리스트 응답 DTO로 변환
	public static LessonApplicationListResponseDto toListResponseDto(
		List<LessonApplication> applications, Long totalCount) {

		// 각 신청을 응답 DTO로 변환
		List<LessonApplicationResponseDto> applicationDtos = applications.stream()
			.map(LessonApplicationMapper::toResponseDto)
			.toList();

		// 리스트 응답 DTO 생성
		return LessonApplicationListResponseDto.builder()
			.lessonApplications(applicationDtos)
			.count(totalCount)
			.build();
	}
}
