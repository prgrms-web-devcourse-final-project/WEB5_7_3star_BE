package com.threestar.trainus.domain.lesson.admin.mapper;

import java.util.List;

import com.threestar.trainus.domain.lesson.admin.dto.ParticipantDto;
import com.threestar.trainus.domain.lesson.admin.dto.ParticipantListResponseDto;
import com.threestar.trainus.domain.lesson.admin.entity.LessonApplication;
import com.threestar.trainus.domain.profile.dto.ProfileResponseDto;

public class LessonParticipantMapper {

	//LessonApplication 엔티티를 ParticipantDto로 변환
	public static ParticipantDto toParticipantDto(LessonApplication application) {
		// User 엔티티에서 정보 가져오기
		ProfileResponseDto userDto = new ProfileResponseDto(
			application.getUser().getId(),
			application.getUser().getNickname(),
			application.getUser().getProfile().getProfileImage(),
			application.getUser().getProfile().getIntro()
		);

		return ParticipantDto.builder()
			.lessonApplicationId(application.getId())
			.user(userDto)
			.joinedAt(application.getCreatedAt()) // 신청 승인된 시간 -> 참가 시간으로 사용
			.build();
	}

	//참가자 목록과 전체 개수를 응답 DTO로 변환
	public static ParticipantListResponseDto toParticipantsResponseDto(
		List<LessonApplication> participants, Long totalCount) {

		// 각 참가자를 DTO로 변환
		List<ParticipantDto> participantDtos = participants.stream()
			.map(LessonParticipantMapper::toParticipantDto)
			.toList();

		// 응답 DTO 생성
		return ParticipantListResponseDto.builder()
			.lessonApplications(participantDtos)
			.count(totalCount)
			.build();
	}
}
