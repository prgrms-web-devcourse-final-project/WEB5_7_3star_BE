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
// public class KeywordSearchPerformanceTest {
//
//     @Autowired
//     private LessonRepository lessonRepository;
//
//     private static final String SEARCH_KEYWORD = "유니크";
//     private static final int PAGE_SIZE = 10;
//
//     @Test
//     @DisplayName("성능 측정: 키워드 검색 (동일 지역 데이터, Full-Text)")
//     void searchWithKeywordOnSameLocationData() {
//         StopWatch stopWatch = new StopWatch();
//         stopWatch.start();
//
//         int total = lessonRepository.countLessonsWithKeyword(
//             null, "도시5", "구5", "동5", null, SEARCH_KEYWORD, 50 // countLimit
//         );
//         List<Lesson> result = lessonRepository.findLessonsWithKeyword(
//             null, "도시5", "구5", "동5", null, SEARCH_KEYWORD, "LATEST", 0, PAGE_SIZE
//         );
//
//         stopWatch.stop();
//         log.info("[키워드 O, 동일 지역] 검색어: {}, 총 {}건 조회, 실행 시간: {} ms", SEARCH_KEYWORD, total,
//                 stopWatch.getTotalTimeMillis());
//     }
// }
