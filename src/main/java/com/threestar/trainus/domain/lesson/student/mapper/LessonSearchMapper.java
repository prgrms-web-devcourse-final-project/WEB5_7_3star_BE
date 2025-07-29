package com.threestar.trainus.domain.lesson.student.mapper;

import java.util.List;

import com.threestar.trainus.domain.lesson.student.dto.LessonSearchResponseDto;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.metadata.dto.ProfileMetadataResponseDto;
import com.threestar.trainus.domain.profile.entity.Profile;
import com.threestar.trainus.domain.user.entity.User;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LessonSearchMapper {

	public static LessonSearchResponseDto toLessonSearchResponseDto(
		Lesson lesson,
		User leader,
		Profile profile,
		ProfileMetadataResponseDto metadata,
		List<String> lessonImageUrls
	) {
		return new LessonSearchResponseDto(
			lesson.getId(),
			lesson.getLessonName(),
			leader.getNickname(),
			profile.getProfileImage(),
			metadata.reviewCount(),
			metadata.rating(),
			lesson.getCategory(),
			lesson.getPrice(),
			lesson.getMaxParticipants(),
			lesson.getParticipantCount(),
			lesson.getStatus(),
			lesson.getStartAt(),
			lesson.getEndAt(),
			lesson.getOpenTime(),
			lesson.getOpenRun(),
			lesson.getCity(),
			lesson.getDistrict(),
			lesson.getDong(),
			lesson.getRi(),
			lesson.getCreatedAt(),
			lessonImageUrls
		);
	}
}
