package com.threestar.trainus.domain.lesson.student.mapper;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.threestar.trainus.domain.lesson.student.dto.MyLessonApplicationListResponseDto;
import com.threestar.trainus.domain.lesson.student.dto.MyLessonApplicationResponseDto;
import com.threestar.trainus.domain.lesson.teacher.entity.ApplicationStatus;
import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonApplication;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;

class LessonApplicationMapperTest {

	@Test
	@DisplayName("내 레슨 신청 응답은 신청 ID, 레슨 요약, 상태를 매핑한다")
	void toMyLessonApplicationDto_MapsFields() {
		Lesson lesson = createLesson(1L, 10L);
		LessonApplication application = LessonApplication.builder()
			.lesson(lesson)
			.user(createUser(2L))
			.build();
		ReflectionTestUtils.setField(application, "id", 100L);

		MyLessonApplicationResponseDto response = LessonApplicationMapper.toMyLessonApplicationDto(application);

		assertThat(response.lessonApplicationId()).isEqualTo(100L);
		assertThat(response.lesson().id()).isEqualTo(1L);
		assertThat(response.lesson().lessonName()).isEqualTo("테스트 레슨");
		assertThat(response.lesson().lessonLeader()).isEqualTo(10L);
		assertThat(response.lesson().price()).isEqualTo(30000);
		assertThat(response.lesson().addressDetail()).isEqualTo("상세 주소");
		assertThat(response.status()).isEqualTo(ApplicationStatus.PENDING);
	}

	@Test
	@DisplayName("신청 목록 응답은 count와 변환된 신청 목록을 함께 반환한다")
	void toDtoListWithCount_MapsListAndCount() {
		LessonApplication first = LessonApplication.builder()
			.lesson(createLesson(1L, 10L))
			.user(createUser(2L))
			.build();
		LessonApplication second = LessonApplication.builder()
			.lesson(createLesson(2L, 11L))
			.user(createUser(2L))
			.build();
		ReflectionTestUtils.setField(first, "id", 100L);
		ReflectionTestUtils.setField(second, "id", 101L);

		MyLessonApplicationListResponseDto response = LessonApplicationMapper.toDtoListWithCount(
			List.of(first, second), 5
		);

		assertThat(response.count()).isEqualTo(5);
		assertThat(response.lessonApplications()).extracting(MyLessonApplicationResponseDto::lessonApplicationId)
			.containsExactly(100L, 101L);
	}

	private Lesson createLesson(Long lessonId, Long leaderId) {
		LocalDateTime startAt = LocalDateTime.now().plusDays(2);
		Lesson lesson = Lesson.builder()
			.lessonLeader(leaderId)
			.lessonName("테스트 레슨")
			.description("설명")
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
		ReflectionTestUtils.setField(lesson, "id", lessonId);
		return lesson;
	}

	private User createUser(Long userId) {
		return User.builder()
			.id(userId)
			.email("test" + userId + "@test.com")
			.nickname("테스트유저" + userId)
			.role(UserRole.USER)
			.build();
	}
}
