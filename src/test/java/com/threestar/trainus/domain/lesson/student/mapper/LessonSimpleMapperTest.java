package com.threestar.trainus.domain.lesson.student.mapper;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.threestar.trainus.domain.lesson.student.dto.LessonSimpleResponseDto;
import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;

class LessonSimpleMapperTest {

	@Test
	@DisplayName("간단 레슨 DTO는 핵심 예약 정보를 매핑한다")
	void toLessonSimpleDto_MapsLessonFields() {
		LocalDateTime startAt = LocalDateTime.now().plusDays(2);
		Lesson lesson = Lesson.builder()
			.lessonLeader(10L)
			.lessonName("테스트 레슨")
			.description("레슨 설명")
			.category(Category.GYM)
			.price(30000)
			.maxParticipants(10)
			.startAt(startAt)
			.endAt(startAt.plusHours(2))
			.openRun(false)
			.city("서울시")
			.district("강남구")
			.dong("역삼동")
			.address("테스트 주소")
			.addressDetail("상세 주소")
			.build();
		ReflectionTestUtils.setField(lesson, "id", 1L);

		LessonSimpleResponseDto response = LessonSimpleMapper.toLessonSimpleDto(lesson);

		assertThat(response.lessonId()).isEqualTo(1L);
		assertThat(response.lessonName()).isEqualTo("테스트 레슨");
		assertThat(response.startAt()).isEqualTo(startAt);
		assertThat(response.endAt()).isEqualTo(startAt.plusHours(2));
		assertThat(response.price()).isEqualTo(30000L);
		assertThat(response.addressDetail()).isEqualTo("상세 주소");
	}
}
