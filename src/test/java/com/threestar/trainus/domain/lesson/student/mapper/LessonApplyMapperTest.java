package com.threestar.trainus.domain.lesson.student.mapper;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.threestar.trainus.domain.lesson.student.dto.LessonApplicationResponseDto;
import com.threestar.trainus.domain.lesson.teacher.entity.ApplicationStatus;

class LessonApplyMapperTest {

	@Test
	@DisplayName("레슨 신청 응답 DTO는 레슨, 사용자, 상태, 신청 시간을 매핑한다")
	void toLessonApplicationResponseDto_MapsFields() {
		LocalDateTime appliedAt = LocalDateTime.now();

		LessonApplicationResponseDto response = LessonApplyMapper.toLessonApplicationResponseDto(
			1L, 2L, ApplicationStatus.APPROVED, appliedAt
		);

		assertThat(response.lessonId()).isEqualTo(1L);
		assertThat(response.userId()).isEqualTo(2L);
		assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED);
		assertThat(response.appliedAt()).isEqualTo(appliedAt);
	}
}
