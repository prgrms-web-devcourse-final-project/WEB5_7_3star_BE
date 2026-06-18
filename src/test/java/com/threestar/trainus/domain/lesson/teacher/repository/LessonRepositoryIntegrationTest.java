package com.threestar.trainus.domain.lesson.teacher.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;

import com.threestar.trainus.domain.lesson.student.entity.LessonSortType;
import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.testsupport.PostgresIntegrationTestSupport;

@DisplayName("레슨 Repository 통합 테스트")
class LessonRepositoryIntegrationTest extends PostgresIntegrationTestSupport {

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private GeometryFactory geometryFactory;

	@AfterEach
	void cleanDatabase() {
		lessonRepository.deleteAllInBatch();
	}

	@Test
	@DisplayName("주소 기반 검색은 지역 조건과 페이징, count를 함께 만족한다")
	void findLessonsWithoutFullText_appliesRegionFiltersPagingAndCount() {
		saveLesson("가격 낮은 요가", 12000, Category.GYM, "서울시", "강남구", "역삼동", "");
		saveLesson("가격 높은 요가", 25000, Category.GYM, "서울시", "강남구", "역삼동", "");
		saveLesson("다른 지역 레슨", 9000, Category.GYM, "서울시", "서초구", "서초동", "");

		int count = lessonRepository.countLessonsWithoutFullText(
			Category.GYM.name(),
			"서울시",
			"강남구",
			"역삼동",
			"",
			50
		);

		List<Lesson> firstPage = lessonRepository.findLessonsWithoutFullText(
			Category.GYM.name(),
			"서울시",
			"강남구",
			"역삼동",
			"",
			LessonSortType.PRICE_LOW.name(),
			0,
			1
		);

		List<Lesson> secondPage = lessonRepository.findLessonsWithoutFullText(
			Category.GYM.name(),
			"서울시",
			"강남구",
			"역삼동",
			"",
			LessonSortType.PRICE_LOW.name(),
			1,
			1
		);

		assertThat(count).isEqualTo(2);
		assertThat(firstPage).extracting(Lesson::getLessonName).containsExactly("가격 낮은 요가");
		assertThat(secondPage).extracting(Lesson::getLessonName).containsExactly("가격 높은 요가");
	}

	@Test
	@DisplayName("키워드 검색은 결과와 count를 실제 DB 기준으로 맞춘다")
	void findLessonsWithFullText_appliesKeywordFiltersAndCount() {
		saveLesson("요가 초급", 18000, Category.GYM, "서울시", "강남구", "역삼동", "");
		saveLesson("요가 심화", 26000, Category.GYM, "서울시", "강남구", "역삼동", "");
		saveLesson("필라테스", 22000, Category.GYM, "서울시", "강남구", "역삼동", "");

		int count = lessonRepository.countLessonsWithFullText(
			Category.GYM.name(),
			"서울시",
			"강남구",
			"역삼동",
			"",
			"요가",
			50
		);

		List<Lesson> lessons = lessonRepository.findLessonsWithFullText(
			Category.GYM.name(),
			"서울시",
			"강남구",
			"역삼동",
			"",
			"요가",
			LessonSortType.PRICE_HIGH.name(),
			0,
			10
		);

		assertThat(count).isEqualTo(2);
		assertThat(lessons).extracting(Lesson::getLessonName).containsExactly("요가 심화", "요가 초급");
		assertThat(lessons).extracting(Lesson::getPrice).containsExactly(26000, 18000);
	}

	@Test
	@DisplayName("위치 기반 검색은 거리와 count를 실제 PostGIS 기준으로 맞춘다")
	void findLessonsByLocationWithoutKeyword_appliesDistanceFiltersAndCount() {
		Point center = point(126.9780, 37.5665);
		Point near = point(126.9790, 37.5665);
		Point far = point(127.0500, 37.6000);

		saveLesson("가까운 레슨", 14000, Category.GYM, "서울시", "강남구", "역삼동", "", center);
		saveLesson("조금 먼 레슨", 17000, Category.GYM, "서울시", "강남구", "역삼동", "", near);
		saveLesson("카테고리 다른 레슨", 19000, Category.BADMINTON, "서울시", "강남구", "역삼동", "", near);
		saveLesson("너무 먼 레슨", 22000, Category.GYM, "서울시", "강남구", "역삼동", "", far);

		int count = lessonRepository.countLessonsByLocationWithoutKeyword(
			Category.GYM.name(),
			center,
			1000,
			50
		);

		List<Lesson> lessons = lessonRepository.findLessonsByLocationWithoutKeyword(
			Category.GYM.name(),
			center,
			1000,
			"DISTANCE",
			0,
			10
		);

		assertThat(count).isEqualTo(2);
		assertThat(lessons).extracting(Lesson::getLessonName).containsExactly("가까운 레슨", "조금 먼 레슨");
	}

	private Lesson saveLesson(
		String lessonName,
		int price,
		Category category,
		String city,
		String district,
		String dong,
		String ri
	) {
		return saveLesson(lessonName, price, category, city, district, dong, ri, null);
	}

	private Lesson saveLesson(
		String lessonName,
		int price,
		Category category,
		String city,
		String district,
		String dong,
		String ri,
		Point locationPoint
	) {
		Lesson lesson = Lesson.builder()
			.lessonLeader(1L)
			.lessonName(lessonName)
			.description("레슨 설명")
			.maxParticipants(10)
			.startAt(LocalDateTime.now().plusDays(1))
			.endAt(LocalDateTime.now().plusDays(1).plusHours(2))
			.price(price)
			.category(category)
			.openTime(LocalDateTime.now().minusMinutes(10))
			.openRun(true)
			.city(city)
			.district(district)
			.dong(dong)
			.ri(ri)
			.address("서울시 강남구 역삼동")
			.addressDetail("101호")
			.locationPoint(locationPoint)
			.build();

		return lessonRepository.saveAndFlush(lesson);
	}

	private Point point(double longitude, double latitude) {
		return geometryFactory.createPoint(new Coordinate(longitude, latitude));
	}
}
