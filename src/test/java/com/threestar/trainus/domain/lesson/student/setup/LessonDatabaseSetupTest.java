package com.threestar.trainus.domain.lesson.student.setup;

import com.threestar.trainus.domain.lesson.teacher.entity.Category;
import com.threestar.trainus.domain.lesson.teacher.entity.Lesson;
import com.threestar.trainus.domain.lesson.teacher.repository.LessonRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Disabled("데이터 생성 전용 테스트, 필요할 때만 수동으로 실행")
public class LessonDatabaseSetupTest {

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GeometryFactory geometryFactory;

    @Autowired
    private EntityManager entityManager;

    private static final int DATA_SIZE = 10000;
    private static final int BATCH_SIZE = 10000;

    private void deleteAllTables() {
        log.info("기존 데이터 삭제");
        jdbcTemplate.update("TRUNCATE TABLE comments, lesson_images, lesson_participants, lesson_applications, reviews, payments, lessons RESTART IDENTITY CASCADE");
        log.info("모든 관련 테이블의 데이터 삭제 완료.");
    }

    @Test
    @DisplayName("LOCALE 모드: 2만개 조합의 지역으로 DATA_SIZE 만큼 생성")
    @Transactional
    @Commit
    void seedLocaleData() {
        deleteAllTables();
        log.info("테스트 데이터 생성을 시작 (총 {}건, 모드: LOCALE - 2만 조합)", DATA_SIZE);
        List<Lesson> batch = new ArrayList<>();
        Random random = new Random();

        String[] cities = new String[10];
        String[] districts = new String[10];
        String[] dongs = new String[20];
        String[] ris = new String[10];
        for (int i = 0; i < 10; i++) {
            cities[i] = "도시" + i;
            districts[i] = "구" + i;
            ris[i] = "리" + i;
        }
        for (int i = 0; i < 20; i++) {
            dongs[i] = "동" + i;
        }

        String neutralKeyword = "검색 키워드";
        double neutralKeywordProbability = 0.1;

        for (int i = 1; i <= DATA_SIZE; i++) {
            String lessonName = "일반 레슨 " + i;
            if (random.nextDouble() < neutralKeywordProbability) {
                lessonName = neutralKeyword + " " + lessonName;
            }
            batch.add(createLesson(1L, lessonName, random,
                    cities[random.nextInt(cities.length)],
                    districts[random.nextInt(districts.length)],
                    dongs[random.nextInt(dongs.length)],
                    ris[random.nextInt(ris.length)]));

            if (i % BATCH_SIZE == 0) {
                saveAndClear(batch);
                log.info("{}만 건 데이터 저장 완료...", i / 10000);
            }
        }

        if (!batch.isEmpty()) {
            saveAndClear(batch);
        }
        log.info("총 {}건 테스트 데이터 생성이 완료되었습니다.", DATA_SIZE);
    }

    @Test
    @DisplayName("KEYWORD 모드: 커먼/레어/유니크 키워드 데이터 50만건 생성")
    @Transactional
    @Commit
    void seedKeywordData() {
        deleteAllTables();
        log.info("테스트 데이터 생성을 시작 (총 {}건, 모드: KEYWORD - 커먼/레어/유니크 키워드 포함)", DATA_SIZE);
        List<Lesson> batch = new ArrayList<>();
        Random random = new Random();

        String[] cities = {"도시5"};
        String[] districts = {"구5"};
        String[] dongs = {"동5"};

        int commonCount = 100000;
        int rareCount = 1000;
        int uniqueCount = 1;
        int remaining = DATA_SIZE - (commonCount + rareCount + uniqueCount);

        int totalSaved = 0;

        // 커먼 키워드 생성
        for (int i = 0; i < commonCount; i++) {
            batch.add(createLesson(1L, "커먼" + i, random, cities[0], districts[0], dongs[0], null));
            totalSaved++;
            if (batch.size() == BATCH_SIZE) {
                saveAndClear(batch);
                log.info("{}만 건 데이터 저장 완료...", totalSaved / 10000);
            }
        }

        // 레어 키워드 생성
        for (int i = 0; i < rareCount; i++) {
            batch.add(createLesson(1L, "레어" + i, random, cities[0], districts[0], dongs[0], null));
            totalSaved++;
            if (batch.size() == BATCH_SIZE) {
                saveAndClear(batch);
                log.info("{}만 건 데이터 저장 완료...", totalSaved / 10000);
            }
        }

        // 유니크 키워드 생성
        for (int i = 0; i < uniqueCount; i++) {
            batch.add(createLesson(1L, "유니크" + i, random, cities[0], districts[0], dongs[0], null));
            totalSaved++;
            if (batch.size() == BATCH_SIZE) {
                saveAndClear(batch);
                log.info("{}만 건 데이터 저장 완료...", totalSaved / 10000);
            }
        }

        // 일반 데이터 생성
        for (int i = 0; i < remaining; i++) {
            batch.add(createLesson(1L, "일반" + i, random, cities[0], districts[0], dongs[0], null));
            totalSaved++;
            if (batch.size() == BATCH_SIZE) {
                saveAndClear(batch);
                log.info("{}만 건 데이터 저장 완료...", totalSaved / 10000);
            }
        }

        if (!batch.isEmpty()) {
            saveAndClear(batch);
        }
        log.info("총 {}건 테스트 데이터 저장이 완료되었습니다.", DATA_SIZE);
    }

    @Test
    @DisplayName("DISTANCE 모드: DATA_SIZE 만큼 생성")
    @Transactional
    @Commit
    void seedDistanceData() {
        deleteAllTables();
        log.info("테스트 데이터 생성을 시작 (총 {}건, 모드: DISTANCE - 2만 조합)", DATA_SIZE);
        List<Lesson> batch = new ArrayList<>();
        Random random = new Random();

        // 강남구 역삼동 중심 좌표
        final double CENTER_LON = 127.0368861;
        final double CENTER_LAT = 37.5007861;

        final double[] distances = {0.1, 0.5, 2.0, 4.0, 8.0};
        String neutralKeyword = "검색 키워드";
        double neutralKeywordProbability = 0.1;
        int totalSaved = 0;

        double totalWeight = 0;// 거리에 따른 가중치 계산
        for (double distance : distances) {
            totalWeight += distance;
        }

        // Allocate lessons based on weighted distribution
        int[] lessonsPerDistanceAllocated = new int[distances.length];
        int currentRemainingLessons = DATA_SIZE;

        for (int i = 0; i < distances.length; i++) {
            double proportion = distances[i] / totalWeight;
            if (i < distances.length - 1) {
                lessonsPerDistanceAllocated[i] = (int) (DATA_SIZE * proportion);
                currentRemainingLessons -= lessonsPerDistanceAllocated[i];
            } else {
                lessonsPerDistanceAllocated[i] = currentRemainingLessons;
            }
        }

        for (int dIdx = 0; dIdx < distances.length; dIdx++) {
            double distance = distances[dIdx];
            int numLessonsForThisDistance = lessonsPerDistanceAllocated[dIdx];
            log.info("{}km 거리의 데이터 {}건 생성을 시작합니다.", distance, numLessonsForThisDistance);

            for (int i = 0; i < numLessonsForThisDistance; i++) {
                String lessonName = "역삼동 " + String.format("%.1f", distance) + "km 부근 레슨 " + i;

                // 랜덤 각도
                double angle = 2 * Math.PI * random.nextDouble();

                // 위도 경도 변환 (대략적인 계산)
                // 1도 위도 = 약 111.32km
                // 1도 경도 = 111.32km * cos(위도)
                double latOffset = (distance * 1000.0 / 111320.0) * Math.sin(angle);
                double lonOffset = (distance * 1000.0 / (111320.0 * Math.cos(Math.toRadians(CENTER_LAT)))) * Math.cos(angle);

                double newLat = CENTER_LAT + latOffset;
                double newLon = CENTER_LON + lonOffset;

                // 새로운 Point 생성
                Point locationPoint = geometryFactory.createPoint(new Coordinate(newLon, newLat));

                if (random.nextDouble() < neutralKeywordProbability) {
                    lessonName = neutralKeyword + " " + lessonName;
                }
                batch.add(createLessonWithDistance(1L, lessonName, random, locationPoint));
                if (batch.size() % BATCH_SIZE == 0) {
                    saveAndClear(batch);
                    log.info("{}만 건 데이터 저장 완료...", ++totalSaved);
                }
            }
        }
        if (!batch.isEmpty()) {
            saveAndClear(batch);
        }
        log.info("총 {}건 테스트 데이터 저장이 완료되었습니다.", DATA_SIZE);
    }

    private void saveAndClear(List<Lesson> batch) {
        if (batch.isEmpty()) {
            return;
        }
        lessonRepository.saveAll(batch);
        batch.clear();
        entityManager.flush();
        entityManager.clear();
    }



    private Lesson createLesson(Long leaderId, String lessonName, Random random, String city, String district, String dong, String ri) {
        // address 필드 생성
        StringBuilder addressBuilder = new StringBuilder();
        addressBuilder.append(city).append(" ").append(district).append(" ").append(dong);
        if (ri != null && !ri.isEmpty()) {
            addressBuilder.append(" ").append(ri);
        }
        addressBuilder.append(" ").append(random.nextInt(1000) + 1).append("-").append(random.nextInt(100) + 1); // 임의의 번지 추가
        String address = addressBuilder.toString();

        // locationPoint 생성
        double latitude = 37.0 + random.nextDouble();
        double longitude = 127.0 + random.nextDouble();
        Point locationPoint = geometryFactory.createPoint(new Coordinate(longitude, latitude));

        return Lesson.builder()
                .lessonLeader(leaderId)
                .lessonName(lessonName)
                .description("테스트 설명")
                .maxParticipants(20)
                .startAt(LocalDateTime.now().plusDays(10))
                .endAt(LocalDateTime.now().plusDays(20))
                .price(50000)
                .category(Category.GYM)
                .openTime(LocalDateTime.now())
                .openRun(true)
                .city(city)
                .district(district)
                .dong(dong)
                .ri(ri)
                .address(address)
                .addressDetail("상세 주소 " + random.nextInt(100))
                .locationPoint(locationPoint)
                .build();
    }

    private Lesson createLessonWithDistance(Long leaderId, String lessonName, Random random, Point locationPoint) {
        return Lesson.builder()
                .lessonLeader(leaderId)
                .lessonName(lessonName)
                .description("테스트 설명")
                .maxParticipants(20)
                .startAt(LocalDateTime.now().plusDays(10))
                .endAt(LocalDateTime.now().plusDays(20))
                .price(50000)
                .category(Category.GYM)
                .openTime(LocalDateTime.now())
                .openRun(true)
                .city("임시시")
                .district("임시구")
                .dong("임시동")
                .address("임시시 임시구 임시동")
                .addressDetail("상세 주소 " + random.nextInt(100))
                .locationPoint(locationPoint)
                .build();
    }
}
