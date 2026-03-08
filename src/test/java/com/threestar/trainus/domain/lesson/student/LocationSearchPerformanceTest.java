// package com.threestar.trainus.domain.lesson.student;
//
// import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
// import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
// import lombok.extern.slf4j.Slf4j;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.ActiveProfiles;
// import org.springframework.util.StopWatch;
//
// import java.util.List;
//
// @Slf4j
// @SpringBootTest
// @ActiveProfiles("test")
// public class LocationSearchPerformanceTest {
//
//     @Autowired
//     private LessonRepository lessonRepository;
//
//     private static final String SEARCH_KEYWORD = "검색 키워드";
//     private static final int PAGE_SIZE = 10;
//
//     @Test
//     @DisplayName("성능 측정: 키워드 포함 검색")
//     void searchWithKeyword() {
//         StopWatch stopWatch = new StopWatch();
//         stopWatch.start();
//
//         // Service 로직과 유사하게 count와 list를 별도로 조회
//         int total = lessonRepository.countLessonsWithKeyword(
//             null, "도시5", "구5", "동5", "리5", SEARCH_KEYWORD, 50 // countLimit
//         );
//         List<Lesson> result = lessonRepository.findLessonsWithKeyword(
//             null, "도시5", "구5", "동5", "리5", SEARCH_KEYWORD, "LATEST", 0, PAGE_SIZE
//         );
//
//         stopWatch.stop();
//         log.info("[키워드 O] 검색어: {}, 총 {}건 조회, 실행 시간: {} ms", SEARCH_KEYWORD, total,
//                 stopWatch.getTotalTimeMillis());
//     }
//
//     @Test
//     @DisplayName("성능 측정: 키워드 미포함 검색")
//     void searchWithoutKeyword() {
//         StopWatch stopWatch = new StopWatch();
//         stopWatch.start();
//
//         // Service 로직과 유사하게 count와 list를 별도로 조회
//         int total = lessonRepository.countLessonsWithoutKeyword(
//             null, "도시5", "구5", "동5", "리5", 50 // countLimit
//         );
//         List<Lesson> result = lessonRepository.findLessonsWithoutKeyword(
//             null, "도시5", "구5", "동5", "리5", "LATEST", 0, PAGE_SIZE
//         );
//
//         stopWatch.stop();
//         log.info("[키워드 X] 총 {}건 조회, 실행 시간: {} ms", total,
//                 stopWatch.getTotalTimeMillis());
//     }
// }