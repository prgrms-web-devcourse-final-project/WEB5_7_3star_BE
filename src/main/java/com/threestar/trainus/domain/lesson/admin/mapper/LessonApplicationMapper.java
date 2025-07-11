package com.threestar.trainus.domain.lesson.admin.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.threestar.trainus.domain.lesson.admin.dto.LessonApplicationListResponseDto;
import com.threestar.trainus.domain.lesson.admin.dto.LessonApplicationResponseDto;
import com.threestar.trainus.domain.lesson.admin.dto.UserSimpleDto;
import com.threestar.trainus.domain.lesson.admin.entity.LessonApplication;

@Component
public class LessonApplicationMapper {
	//LessonApplication 엔티티를 LessonApplicationResponseDto로 변환
	public LessonApplicationResponseDto toResponseDto(LessonApplication application) {
		// User 엔티티에서 정보 가져오기
		UserSimpleDto userDto = UserSimpleDto.builder()
			.id(application.getUser().getId())
			.nickname(application.getUser().getNickname())
			.profileImage(application.getUser().getProfile().getProfileImage())
			.build();

		return LessonApplicationResponseDto.builder()
			.lessonApplicationId(application.getId())
			.user(userDto)
			.status(application.getStatus().name())
			.appliedAt(application.getCreatedAt())
			.build();
	}

	//신청자 목록과 전체 개수를 리스트 응답 DTO로 변환
	public LessonApplicationListResponseDto toListResponseDto(
		List<LessonApplication> applications, Long totalCount) {

		// 각 신청을 응답 DTO로 변환
		List<LessonApplicationResponseDto> applicationDtos = applications.stream()
			.map(this::toResponseDto)
			.toList();

		// 리스트 응답 DTO 생성
		return LessonApplicationListResponseDto.builder()
			.lessonApplications(applicationDtos)
			.count(totalCount)
			.build();
	}
}

