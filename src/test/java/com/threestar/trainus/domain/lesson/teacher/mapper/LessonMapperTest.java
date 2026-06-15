package com.threestar.trainus.domain.lesson.teacher.mapper;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.test.util.ReflectionTestUtils;

import com.threestar.trainus.domain.lesson.student.dto.LessonDetailResponseDto;
import com.threestar.trainus.domain.lesson.teacher.dto.LessonCreateRequestDto;
import com.threestar.trainus.domain.lesson.teacher.dto.LessonResponseDto;
import com.threestar.trainus.domain.lesson.teacher.dto.LessonUpdateResponseDto;
import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.entity.LessonImage;
import com.threestar.trainus.domain.profile.entity.Profile;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;

class LessonMapperTest {

	private final GeometryFactory geometryFactory = new GeometryFactory();

	@Test
	@DisplayName("생성 요청 DTO를 레슨 엔티티로 변환한다")
	void toEntity_MapsCreateRequest() {
		LocalDateTime startAt = LocalDateTime.now().plusDays(2);
		LessonCreateRequestDto request = createRequest(startAt);
		User user = createUser(1L);
		Point point = geometryFactory.createPoint(new Coordinate(126.9780, 37.5665));

		Lesson lesson = LessonMapper.toEntity(request, user, point);

		assertThat(lesson.getLessonLeader()).isEqualTo(user.getId());
		assertThat(lesson.getLessonName()).isEqualTo(request.lessonName());
		assertThat(lesson.getCategory()).isEqualTo(Category.GYM);
		assertThat(lesson.getOpenRun()).isTrue();
		assertThat(lesson.getLocationPoint()).isSameAs(point);
		assertThat(lesson.getParticipantCount()).isZero();
	}

	@Test
	@DisplayName("레슨 응답 DTO는 이미지 URL과 좌표를 함께 매핑한다")
	void toResponseDto_MapsImagesAndCoordinates() {
		Lesson lesson = createLesson(1L, 10L);
		List<LessonImage> images = List.of(
			LessonImage.builder().lesson(lesson).imageUrl("https://cdn.test/1.png").build(),
			LessonImage.builder().lesson(lesson).imageUrl("https://cdn.test/2.png").build()
		);

		LessonResponseDto response = LessonMapper.toResponseDto(lesson, images);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.lessonLeader()).isEqualTo(10L);
		assertThat(response.latitude()).isEqualTo(37.5665);
		assertThat(response.longitude()).isEqualTo(126.9780);
		assertThat(response.lessonImages()).containsExactly("https://cdn.test/1.png", "https://cdn.test/2.png");
	}

	@Test
	@DisplayName("레슨 상세 DTO는 리더와 프로필 메타 정보를 포함해 매핑한다")
	void toLessonDetailDto_MapsLeaderProfileAndImages() {
		Lesson lesson = createLesson(1L, 10L);
		User leader = createUser(10L);
		Profile profile = Profile.builder()
			.user(leader)
			.profileImage("https://cdn.test/profile.png")
			.intro("소개")
			.build();

		LessonDetailResponseDto response = LessonMapper.toLessonDetailDto(
			lesson, leader, profile, 3, 4.2, List.of("https://cdn.test/lesson.png")
		);

		assertThat(response.lessonLeaderName()).isEqualTo(leader.getNickname());
		assertThat(response.profileIntro()).isEqualTo("소개");
		assertThat(response.profileImage()).isEqualTo("https://cdn.test/profile.png");
		assertThat(response.reviewCount()).isEqualTo(3);
		assertThat(response.rating()).isEqualTo(4.2);
		assertThat(response.lessonImages()).containsExactly("https://cdn.test/lesson.png");
	}

	@Test
	@DisplayName("레슨 수정 응답 DTO도 이미지 URL과 좌표를 매핑한다")
	void toUpdateResponseDto_MapsImagesAndCoordinates() {
		Lesson lesson = createLesson(1L, 10L);
		List<LessonImage> images = List.of(
			LessonImage.builder().lesson(lesson).imageUrl("https://cdn.test/update.png").build()
		);

		LessonUpdateResponseDto response = LessonMapper.toUpdateResponseDto(lesson, images);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.latitude()).isEqualTo(37.5665);
		assertThat(response.longitude()).isEqualTo(126.9780);
		assertThat(response.lessonImages()).containsExactly("https://cdn.test/update.png");
	}

	private LessonCreateRequestDto createRequest(LocalDateTime startAt) {
		return new LessonCreateRequestDto(
			"테스트 레슨",
			"레슨 설명",
			Category.GYM,
			30000,
			10,
			startAt,
			startAt.plusHours(2),
			startAt.minusHours(1),
			true,
			"서울시",
			"강남구",
			"역삼동",
			"",
			"테스트 주소",
			"상세 주소",
			37.5665,
			126.9780,
			List.of("https://cdn.test/1.png")
		);
	}

	private Lesson createLesson(Long lessonId, Long leaderId) {
		LocalDateTime startAt = LocalDateTime.now().plusDays(2);
		Point point = geometryFactory.createPoint(new Coordinate(126.9780, 37.5665));
		Lesson lesson = Lesson.builder()
			.lessonLeader(leaderId)
			.lessonName("테스트 레슨")
			.description("레슨 설명")
			.category(Category.GYM)
			.price(30000)
			.maxParticipants(10)
			.startAt(startAt)
			.endAt(startAt.plusHours(2))
			.openTime(startAt.minusHours(1))
			.openRun(true)
			.city("서울시")
			.district("강남구")
			.dong("역삼동")
			.ri("")
			.address("테스트 주소")
			.addressDetail("상세 주소")
			.locationPoint(point)
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
