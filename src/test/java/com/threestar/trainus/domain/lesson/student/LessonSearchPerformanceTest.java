package com.threestar.trainus.domain.lesson.student;

import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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

    @PersistenceContext
    private EntityManager entityManager;

    //초기 데이터 생성용
    private static final int DATA_SIZE = 200000;
    private static final String SEARCH_KEYWORD = "요가";
    private static final int INIT_MODE = 2;

    @BeforeAll
    void setUp() {
        if (INIT_MODE == 0) {
            // 동일 주소 데이터 생성
            log.info("테스트 데이터 생성을 시작 (총 {}건)", DATA_SIZE);
            List<Lesson> lessons = new ArrayList<>();
            Random random = new Random();
            String[] cities = {"서울특별시"};
            String[] districts = {"강남구"};
            String[] dongs = {"역삼동"};
            String[] lessonNames = new String[100];
            for (int j = 0; j < 100; j++) {
                if (j == 0) {
                    lessonNames[j] = "강력한 요가";
                } else {
                    lessonNames[j] = "일반 레슨 " + j;
                }
            }
            for (int i = 0; i < DATA_SIZE; i++) {
                lessons.add(Lesson.builder()
                        .lessonLeader(1L)
                        .lessonName(lessonNames[i % lessonNames.length] + " " + i)
                        .description("테스트 설명 " + i)
                        .maxParticipants(20)
                        .startAt(LocalDateTime.now().plusDays(10))
                        .endAt(LocalDateTime.now().plusDays(20))
                        .price(50000)
                        .category(Category.values()[random.nextInt(Category.values().length)])
                        .openTime(LocalDateTime.now())
                        .openRun(true)
                        .city(cities[0])
                        .district(districts[0])
                        .dong(dongs[0])
                        .addressDetail("상세 주소 " + i)
                        .build());
            }
            lessonRepository.saveAll(lessons);
            log.info("테스트 데이터 생성이 완료");
        } else if (INIT_MODE == 1) {
            // 다른 주소 데이터 생성
            log.info("테스트 데이터 생성을 시작 (총 {}건)", DATA_SIZE);
            List<Lesson> lessons = new ArrayList<>();
            Random random = new Random();

            String[] cities = new String[10];
            String[] districts = new String[10];
            String[] dongs = new String[10];
            String[] ris = new String[10];
            for (int i = 0; i < 10; i++) {
                cities[i] = "도시" + i;
                districts[i] = "구" + i;
                dongs[i] = "동" + i;
                ris[i] = "리" + i;
            }

            String[] lessonNames = new String[100];
            for (int j = 0; j < 100; j++) {
                if (j == 0) {
                    lessonNames[j] = "강력한 요가";
                } else {
                    lessonNames[j] = "일반 레슨 " + j;
                }
            }

            for (int i = 0; i < DATA_SIZE; i++) {
                lessons.add(Lesson.builder()
                        .lessonLeader(1L)
                        .lessonName(lessonNames[i % lessonNames.length] + " " + i)
                        .description("테스트 설명 " + i)
                        .maxParticipants(20)
                        .startAt(LocalDateTime.now().plusDays(10))
                        .endAt(LocalDateTime.now().plusDays(20))
                        .price(50000)
                        .category(Category.values()[random.nextInt(Category.values().length)])
                        .openTime(LocalDateTime.now())
                        .openRun(true)
                        .city(cities[random.nextInt(cities.length)])
                        .district(districts[random.nextInt(districts.length)])
                        .dong(dongs[random.nextInt(dongs.length)])
                        .ri(ris[random.nextInt(ris.length)])
                        .addressDetail("상세 주소 " + i)
                        .build());
            }
            lessonRepository.saveAll(lessons);
            log.info("테스트 데이터 생성이 완료");
        } else {
            log.info("데이터를 생성하지 않습니다.");
        }
    }

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
    @DisplayName("성능 측정: 지역으로만 검색")
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
}