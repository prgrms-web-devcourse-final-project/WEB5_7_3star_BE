package com.threestar.trainus.domain.lesson.teacher.mapper;

import java.util.List;

import com.threestar.trainus.domain.lesson.teacher.dto.ParticipantDto;
import com.threestar.trainus.domain.lesson.teacher.dto.ParticipantListResponseDto;
import com.threestar.trainus.domain.lesson.teacher.dto.ParticipantListWrapperDto;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonParticipant;
import com.threestar.trainus.domain.profile.dto.ProfileResponseDto;

public class LessonParticipantMapper {

	//LessonApplication 엔티티를 ParticipantDto로 변환
	public static ParticipantDto toParticipantDto(LessonParticipant participant) {
		// User 엔티티에서 정보 가져오기
		ProfileResponseDto userDto = new ProfileResponseDto(
			participant.getUser().getId(),
			participant.getUser().getNickname(),
			participant.getUser().getProfile().getProfileImage(),
			participant.getUser().getProfile().getIntro()
		);

		return ParticipantDto.builder()
			.lessonApplicationId(participant.getId())
			.user(userDto)
			.participantStatus(participant.getStatus())
			.joinedAt(participant.getJoinAt())
			.build();
	}

	//참가자 목록과 전체 개수를 응답 DTO로 변환
	public static ParticipantListResponseDto toParticipantsResponseDto(
		List<LessonParticipant> participants, int totalCount) {

		// 각 참가자를 DTO로 변환
		List<ParticipantDto> participantDtos = participants.stream()
			.map(LessonParticipantMapper::toParticipantDto)
			.toList();

		return ParticipantListResponseDto.builder()
			.lessonApplications(participantDtos)
			.count(totalCount)
			.build();
	}

	public static ParticipantListWrapperDto toParticipantListWrapperDto(
		ParticipantListResponseDto responseDto) {
		return new ParticipantListWrapperDto(responseDto.lessonApplications());
	}
}

