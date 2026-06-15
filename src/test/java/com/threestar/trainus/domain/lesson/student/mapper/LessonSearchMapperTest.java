package com.threestar.trainus.domain.lesson.student.mapper;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.test.util.ReflectionTestUtils;

import com.threestar.trainus.domain.lesson.student.dto.LessonSearchResponseDto;
import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.metadata.dto.ProfileMetadataResponseDto;
import com.threestar.trainus.domain.profile.entity.Profile;
import com.threestar.trainus.domain.user.entity.User;
import com.threestar.trainus.domain.user.entity.UserRole;

class LessonSearchMapperTest {

	@Test
	@DisplayName("레슨 검색 응답은 리더, 프로필, 메타데이터, 좌표, 이미지 URL을 매핑한다")
	void toLessonSearchResponseDto_MapsFields() {
		LocalDateTime startAt = LocalDateTime.now().plusDays(2);
		Point point = new GeometryFactory().createPoint(new Coordinate(126.9780, 37.5665));
		Lesson lesson = Lesson.builder()
			.lessonLeader(10L)
			.lessonName("검색 레슨")
			.description("설명")
			.category(Category.YOGA)
			.price(20000)
			.maxParticipants(8)
			.startAt(startAt)
			.endAt(startAt.plusHours(2))
			.openTime(startAt.minusHours(1))
			.openRun(false)
			.city("서울시")
			.district("강남구")
			.dong("역삼동")
			.ri("")
			.address("테스트 주소")
			.addressDetail("상세 주소")
			.locationPoint(point)
			.build();
		ReflectionTestUtils.setField(lesson, "id", 1L);
		ReflectionTestUtils.setField(lesson, "participantCount", 3);
		User leader = User.builder()
			.id(10L)
			.email("leader@test.com")
			.nickname("리더")
			.role(UserRole.USER)
			.build();
		Profile profile = Profile.builder()
			.user(leader)
			.profileImage("https://cdn.test/profile.png")
			.intro("소개")
			.build();
		ProfileMetadataResponseDto metadata = new ProfileMetadataResponseDto(10L, 5, 4.8);

		LessonSearchResponseDto response = LessonSearchMapper.toLessonSearchResponseDto(
			lesson, leader, profile, metadata, List.of("https://cdn.test/lesson.png")
		);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.lessonName()).isEqualTo("검색 레슨");
		assertThat(response.lessonLeaderName()).isEqualTo("리더");
		assertThat(response.lessonLeaderImage()).isEqualTo("https://cdn.test/profile.png");
		assertThat(response.reviewCount()).isEqualTo(5);
		assertThat(response.rating()).isEqualTo(4.8);
		assertThat(response.latitude()).isEqualTo(37.5665);
		assertThat(response.longitude()).isEqualTo(126.9780);
		assertThat(response.lessonImages()).containsExactly("https://cdn.test/lesson.png");
	}
}
