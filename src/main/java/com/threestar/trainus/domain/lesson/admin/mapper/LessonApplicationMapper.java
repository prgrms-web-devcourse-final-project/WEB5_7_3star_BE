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
		// TODO: User 도메인 완성 후 ProfileDto 사용으로 변경
		// UserProfileDto userProfileDto = userService.getUserProfile(application.getUser().getId());

		// 일단, 임시 UserSimpleDto로
		UserSimpleDto userDto = UserSimpleDto.builder()
			.id(application.getUserId()) // TODO: User 엔티티 완성 후 application.getUser().getId()로 변경
			.nickname("사용자" + application.getUserId()) // TODO: UserProfileDto의 nickname 필드로 변경
			.profileImage("https://default-profile.com/default.jpg") // TODO: UserProfileDto의 profileImage 필드로 변경
			.build();

		return LessonApplicationResponseDto.builder()
			.lessonApplicationId(application.getId())
			.user(userDto) // TODO: ProfileDto로 변경
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
			.totalCount(totalCount)
			.build();
	}
}
