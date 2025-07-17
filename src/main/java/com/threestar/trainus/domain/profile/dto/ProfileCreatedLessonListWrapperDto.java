package com.threestar.trainus.domain.profile.dto;

import java.util.List;

public record ProfileCreatedLessonListWrapperDto(
	List<ProfileCreatedLessonDto> lessons
) {
}
