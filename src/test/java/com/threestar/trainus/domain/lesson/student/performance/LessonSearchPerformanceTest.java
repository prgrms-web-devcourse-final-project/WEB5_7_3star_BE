package com.threestar.trainus.domain.lesson.student.performance;

import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort; // Sort import 추가
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LessonSearchPerformanceTest {

    @Autowired
    private LessonRepository lessonRepository;

    private static final String SEARCH_KEYWORD = "요가";

    @Test
    @DisplayName("성능 측정: LIKE 검색")
    void searchWithLike() {

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Page<Lesson> result = lessonRepository.findByLocationAndSearchWithLike(
            null,"서울특별시", "강남구", "역삼동", null, SEARCH_KEYWORD, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        stopWatch.stop();
        log.info("[LIKE 검색] 검색어: {}, 총 {}건 조회, 실행 시간: {} ms", SEARCH_KEYWORD, result.getTotalElements(),
                stopWatch.getTotalTimeMillis());
    }

    @Test
    @DisplayName("성능 측정: Full-Text 검색 최적화 (서브쿼리 JOIN 방식)")
    void searchWithOptimizedFullText() {

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Page<Lesson> result = lessonRepository.findByLocationAndFullTextSearchOptimized(
            null, "서울특별시", "강남구", "역삼동", SEARCH_KEYWORD, null, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "created_at"))
        );

        stopWatch.stop();
        log.info("[Full-Text 최적화 검색] 검색어: {}, 총 {}건 조회, 실행 시간: {} ms", SEARCH_KEYWORD, result.getTotalElements(),
                stopWatch.getTotalTimeMillis());
    }

    @Test
    @DisplayName("성능 측정: 법정동으로만 검색")
    void searchByLocation() {

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Page<Lesson> result = lessonRepository.findByLocation(
            null, "도시5", "구5", "동5", "리5", PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        stopWatch.stop();
        log.info("[지역으로만 검색] 총 {}건 조회, 실행 시간: {} ms", result.getTotalElements(),
                stopWatch.getTotalTimeMillis());
    }

    @Test
    @DisplayName("성능 측정: 거리 기반 검색 (ST_DWithin)")
    void searchByDistance() {
        // 서울시 강남구 역삼동 기준 좌표
        final double CENTER_LON = 127.0368861;
        final double CENTER_LAT = 37.5007861;
        final int DISTANCE_METER = 400; // 검색 반경 (미터 단위)

        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        final Point searchPoint = geometryFactory.createPoint(new Coordinate(CENTER_LON, CENTER_LAT));
        final PageRequest pageRequest = PageRequest.of(0, 10);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // 실제 레슨 목록 조회 (페이지 제한 적용)
        List<Lesson> lessons = lessonRepository.findLessonsByLocationWithoutKeyword(
                null,
                searchPoint,
                DISTANCE_METER,
                "DISTANCE",
                (int)pageRequest.getOffset(),
                pageRequest.getPageSize()
        );

        int totalCount = lessonRepository.countLessonsByLocationWithoutKeyword(
                null,
                searchPoint,
                DISTANCE_METER,
                Integer.MAX_VALUE // 전체 카운트를 위해 충분히 큰 값 사용
        );
        
        stopWatch.stop();
        log.info("[거리 기반 검색] 반경: {}m, 조회된 레슨 수 (현재 페이지): {}, 총 검색 결과: {}, 실행 시간: {} ms", 
                DISTANCE_METER, lessons.size(), totalCount, stopWatch.getTotalTimeMillis());
    }
}
