# P2 통합 테스트 후보 조사 보고서

조사 범위:
- PostgreSQL/PostGIS 레슨 검색 쿼리
- 레슨 재고 보정 스케줄러
- 프로필 메타데이터 스케줄러

조사 기준 문서:
- `docs/ai-test-workflow/AGENTS.md`
- `docs/ai-test-workflow/rules/agent-roles.md`
- `docs/ai-test-workflow/rules/human-approval-policy.md`
- `docs/ai-test-workflow/rules/test-boundary.md`
- `docs/ai-test-workflow/rules/enforcement-checklist.md`

## 1) 요약 판단

| 후보 | 현재 테스트 존재 여부 | 통합 테스트 필요성 | Testcontainers 적합성 | 비어 있는 통합 공백 |
| --- | --- | --- | --- | --- |
| PostgreSQL/PostGIS 레슨 검색 쿼리 | 있음. 단, 서비스 단위 mock 위주 | 높음 | 높음 | 실제 DB에서 검색 정확성, 페이지/카운트, 지역 조건, 거리 검색 미검증 |
| 레슨 재고 보정 스케줄러 | 부분 있음. happy path E2E만 존재 | 매우 높음 | 높음 | 보정 실행 조건, busy/lastActive 경계, Redis/DB 정합성, 중복 실행 경계 미검증 |
| 프로필 메타데이터 스케줄러 | 부분 있음. 서비스 단위와 리뷰 생성 E2E는 있음 | 높음 | 높음 | 스케줄러 순회, 리뷰 집계 쿼리, DB 반영, 예외 격리 미검증 |

## 2) 후보별 조사

### A. PostgreSQL/PostGIS 레슨 검색 쿼리

현재 구현 근거:
- 서비스 진입점은 `src/main/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonService.java:83`, `:139`
- 실제 쿼리는 `src/main/java/com/threestar/trainus/domain/lesson/teacher/repository/LessonRepository.java:306`, `:331`, `:357`, `:380`, `:406`, `:424`, `:446`, `:466`
- 위치 컬럼은 `src/main/java/com/threestar/trainus/domain/lesson/teacher/entity/Lesson.java:96`의 `geography(Point, 4326)` 이다
- 카운트 윈도우 계산은 `src/main/java/com/threestar/trainus/global/utils/PageLimitCalculator.java:9`

현재 테스트 존재 여부:
- `src/test/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonServiceTest.java:419`, `:430`, `:447`, `:478`, `:489`
  - 정렬 null 예외
  - 검색어 없음 분기
  - 검색어 있음 분기
  - 위치 검색 정렬 null 예외
  - 위치 검색 검색어 없음 분기
- `src/test/java/com/threestar/trainus/domain/lesson/student/LessonSearchPerformanceTest.java:46`, `:62`, `:78`, `:102`
  - 성능 측정만 하고 assert가 없다
- `src/test/java/com/threestar/trainus/domain/lesson/student/KeywordSearchPerformanceTest.java`와 `LocationSearchPerformanceTest.java`는 주석 처리 상태라 실행 커버리지로 볼 수 없다

통합 테스트가 필요한 이유:
- 현재 단위 테스트는 `lessonRepository` 호출 분기만 확인하고, native query 결과 자체는 검증하지 않는다
- `findLessonsWithFullText` / `countLessonsWithFullText`는 페이지 결과와 count 윈도우를 함께 맞춰야 하며, `countLimit`은 `PageLimitCalculator`에 의해 잘린다
- `findLessonsByLocationWithKeyword` / `findLessonsByLocationWithoutKeyword`는 `ST_Dwithin`과 거리 정렬, 지역 필터를 실제 PostGIS에서 검증해야 한다
- `ri`는 null일 때만 생략되고, 빈 문자열은 별도 값으로 취급되므로 지역 조건의 null/empty 차이를 고정할 필요가 있다

단위 테스트와의 경계:
- 단위 테스트는 `sortBy == null`, 검색어 유무, repository 호출 분기, DTO 매핑 호출 정도까지만 적합하다
- 실제 SQL correctness, 페이징 offset, count 정확도, 지역 필터 조합, 거리 검색 반경은 통합 테스트 경계다

Testcontainers 적합성:
- 매우 적합하다
- `src/test/java/com/threestar/trainus/testsupport/PostgresIntegrationTestSupport.java`가 PostgreSQL/PostGIS와 `integration-test` 프로필을 이미 준비한다
- H2로는 `ST_Dwithin`, geography 컬럼, native PostgreSQL 동작을 검증할 수 없다

데이터 setup / cleanup:
- setup: 최소한 `user`, `profile`, `profile_metadata`, `lesson`, `lesson_image`를 seed 하거나, repository 수준이면 `lesson` 중심으로 seed
- search correctness용 데이터는 동일 지역 내/외, 키워드 일치/불일치, `ri` 유무, 거리 내/외 데이터를 같이 둬야 한다
- cleanup: `deleteAllInBatch` 또는 truncate로 각 테스트 후 테이블을 정리해야 한다

후속 분리 후보:
- repository 쿼리 검증과 service DTO 매핑 검증을 분리하는 편이 좋다
- keyword search와 location search는 서로 다른 통합 테스트 클래스로 나누는 편이 유지보수에 유리하다

### B. 레슨 재고 보정 스케줄러

현재 구현 근거:
- 스케줄러 진입점은 `src/main/java/com/threestar/trainus/domain/lesson/issue/LessonStockReconciliationScheduler.java:41`
- 중복 실행 방지 경계는 `:39`, `:40`의 `@Scheduled` + `@SchedulerLock(name = "LessonStockReconciliation", ...)`
- 보정 전 대기 조건은 `:43`, `:49`, `:73`, `:85`, `:106`, `:115`, `:158`, `:179`, `:210`
- 소비자 busy 카운터 갱신은 `src/main/java/com/threestar/trainus/domain/lesson/issue/LessonApplyConsumer.java:48`, `:49`, `:126`, `:127`

현재 테스트 존재 여부:
- `src/test/java/com/threestar/trainus/domain/lesson/issue/LessonRedisStreamIntegrationTest.java:65`, `:93`, `:100`
  - 레슨 신청 happy path 이후 수동으로 `reconcileStock()`를 호출한다
- `src/test/java/com/threestar/trainus/domain/lesson/issue/LessonRedisStreamFailureIntegrationTest.java:61`, `:87`, `:111`, `:142`
  - producer/admission 실패 경로를 검증하지만 보정 스케줄러 자체는 검증하지 않는다

통합 테스트가 필요한 이유:
- 이 로직은 DB의 실제 참가자 수와 Redis stock, dirty set, waiting room, stream backlog, busy key, lastActive를 동시에 읽는다
- `updateParticipantCount` 호출 후 Redis stock을 다시 맞추는지, dirty set에서 제거하는지, 그리고 보정이 실행되면 안 되는 상태에서 멈추는지 실제 저장소로 봐야 한다
- `busyCount > 0`, `isRecent`, `isStale`, `isNegativeRedisStock`는 소비자 배치 처리 타이밍과 맞물려 있어 mock만으로는 회귀를 고정하기 어렵다

단위 테스트와의 경계:
- `isNegativeRedisStock` 같은 순수 보조 로직은 단위 테스트 대상이 될 수 있다
- 그러나 보정 실행 여부, Redis/DB 정합성, stream lag 판단, dirty set 정리는 통합 테스트가 맞다
- `@SchedulerLock`의 distributed lock 자체는 단위 테스트로 검증할 수 없고, 최소한의 통합 sanity check 정도만 가능하다

Testcontainers 적합성:
- 매우 적합하다
- `src/test/java/com/threestar/trainus/testsupport/RedisStreamIntegrationTestSupport.java`가 PostgreSQL + Redis를 이미 띄우고, Redis cleanup까지 제공한다
- 이 후보는 Redis와 PostgreSQL 둘 다 필요하므로 Testcontainers가 사실상 전제다

데이터 setup / cleanup:
- setup: lesson 1개, participant count와 DB 실제 count 불일치 케이스, dirty set membership, waiting room zset, mq stream pending 상태, busy count, lastActive, redis stock 음수/양수 케이스를 준비해야 한다
- cleanup: DB 테이블(`lesson`, `lesson_participant`, `user` 등)과 Redis key space를 모두 정리해야 한다
- 기존 support는 Redis를 `@AfterEach`로 정리하므로, DB만 추가로 정리하면 된다

배치-consumer 처리 중 보정 타이밍:
- `LessonApplyConsumer`는 `onMessage()`에서 busy count를 올리고 `processBatch()`의 `finally`에서 busy count를 내린다
- 현재 happy path 통합 테스트는 busy key를 지운 뒤 보정한다. 즉, 보정 연기 조건(`busy > 0`, `recent activity`)은 아직 비어 있다
- 이 경계는 이번 후보 중 가장 회귀 위험이 높다

후속 분리 후보:
- 보정 실행 금지 조건 검증과 실제 재고/DB 동기화 검증을 분리하는 편이 좋다
- busy/lastActive 경계, negative stock 경계, stream lag 경계는 서로 별도 케이스로 나누는 편이 좋다

### C. 프로필 메타데이터 스케줄러

현재 구현 근거:
- 배치 갱신 로직은 `src/main/java/com/threestar/trainus/domain/metadata/service/ProfileMetadataService.java:47`
- 스케줄러 진입점은 `src/main/java/com/threestar/trainus/domain/metadata/scheduler/ProfileMetadataScheduler.java:25`, `:28`, `:32`
- 리뷰 집계 쿼리는 `src/main/java/com/threestar/trainus/domain/review/repository/ReviewRepository.java:42`, `:45`

현재 테스트 존재 여부:
- `src/test/java/com/threestar/trainus/domain/metadata/service/ProfileMetadataServiceTest.java:48`, `:78`, `:108`, `:122`, `:157`, `:186`
  - `batchUpdateMetadata()`와 `getMetadata()`를 mock 기반으로만 검증한다
- `src/test/java/com/threestar/trainus/domain/review/controller/ReviewControllerTest.java:119`, `:151`, `:152`, `:153`, `:168`, `:169`, `:170`
  - 리뷰 생성 후 `profile_metadata.reviewCount`와 `rating`이 실제 DB에 반영되는지 확인한다

통합 테스트가 필요한 이유:
- `ProfileMetadataService`는 `ReviewRepository.countByRevieweeId()`와 `findAverageRatingByRevieweeId()` 결과를 그대로 저장한다
- 현재 단위 테스트는 집계값을 stub으로 주기 때문에, 실제 JPQL/DB 계산식과 rounding(`ROUND(AVG(...), 2)`)을 검증하지 못한다
- 스케줄러는 모든 사용자에 대해 순회하며 `batchUpdateMetadata()`를 호출하므로, 한 사용자 실패가 다른 사용자 갱신을 막지 않는지도 DB 기준으로 봐야 한다

단위 테스트와의 경계:
- `reviewCount`가 다를 때 새 엔티티를 만들어 save 하는 분기, rating만 다른 경우 set 하는 분기는 단위 테스트로 충분하다
- 실제 리뷰 집계 값, null 평균 처리, save 후 DB side effect, 여러 사용자 순회는 통합 테스트 경계다

Testcontainers 적합성:
- 높다
- 이 후보는 PostgreSQL만 있으면 충분하고 Redis는 필요 없다
- `src/test/java/com/threestar/trainus/testsupport/PostgresIntegrationTestSupport.java`를 그대로 사용할 수 있다

데이터 setup / cleanup:
- setup: reviewee user, reviewer user들, `profile_metadata` 초기 row, 리뷰 0건/1건/복수건 케이스를 seed
- cleanup: `review`, `profile_metadata`, `user`, `lesson` 관련 테이블을 테스트 후 정리
- 평균 평점이 없는 사용자에 대해 `null -> 0.0` 보정도 실제 DB로 확인해야 한다

후속 분리 후보:
- `ReviewRepository` 집계 쿼리 검증과 `ProfileMetadataScheduler` 순회/예외 격리 검증을 분리하는 편이 좋다
- 리뷰가 없는 사용자, 리뷰가 여러 개인 사용자, 한 사용자 실패 시 다른 사용자 계속 진행 케이스를 별도 케이스로 나누면 좋다

## 3) 성능 테스트 분리 판단

- `src/test/java/com/threestar/trainus/domain/lesson/student/LessonSearchPerformanceTest.java`는 `StopWatch`와 `log.info`만 있고 assert가 없다
- `src/test/java/com/threestar/trainus/domain/lesson/student/KeywordSearchPerformanceTest.java`와 `LocationSearchPerformanceTest.java`는 주석 처리되어 있다
- 따라서 이들은 회귀 테스트 후보가 아니라 성능 벤치마크/데이터 생성 보조로 분리해서 봐야 한다
- `src/test/java/com/threestar/trainus/domain/lesson/student/LessonDatabaseSetupTest.java`는 대량 데이터 seed 도구에 가깝고, 회귀 보장 근거로 쓰면 안 된다

## 4) 조사한 파일

- `src/main/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonService.java`
- `src/main/java/com/threestar/trainus/domain/lesson/teacher/repository/LessonRepository.java`
- `src/main/java/com/threestar/trainus/domain/lesson/issue/LessonStockReconciliationScheduler.java`
- `src/main/java/com/threestar/trainus/domain/metadata/service/ProfileMetadataService.java`
- `src/main/java/com/threestar/trainus/domain/metadata/scheduler/ProfileMetadataScheduler.java`
- `src/main/java/com/threestar/trainus/domain/review/repository/ReviewRepository.java`
- `src/main/java/com/threestar/trainus/domain/lesson/issue/LessonApplyConsumer.java`
- `src/main/java/com/threestar/trainus/domain/lesson/teacher/entity/Lesson.java`
- `src/main/java/com/threestar/trainus/global/utils/PageLimitCalculator.java`
- `src/test/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonServiceTest.java`
- `src/test/java/com/threestar/trainus/domain/lesson/student/LessonSearchPerformanceTest.java`
- `src/test/java/com/threestar/trainus/domain/lesson/student/LocationSearchPerformanceTest.java`
- `src/test/java/com/threestar/trainus/domain/lesson/student/KeywordSearchPerformanceTest.java`
- `src/test/java/com/threestar/trainus/domain/lesson/student/LessonDatabaseSetupTest.java`
- `src/test/java/com/threestar/trainus/domain/metadata/service/ProfileMetadataServiceTest.java`
- `src/test/java/com/threestar/trainus/domain/lesson/issue/LessonRedisStreamIntegrationTest.java`
- `src/test/java/com/threestar/trainus/domain/lesson/issue/LessonRedisStreamFailureIntegrationTest.java`
- `src/test/java/com/threestar/trainus/domain/review/controller/ReviewControllerTest.java`
- `src/test/java/com/threestar/trainus/testsupport/PostgresIntegrationTestSupport.java`
- `src/test/java/com/threestar/trainus/testsupport/RedisStreamIntegrationTestSupport.java`

## 5) 실행 기록 요약

- 프롬프트: P2 통합 테스트 후보 조사, 지정된 3개 범위에 대해 현재 테스트 존재 여부와 통합 테스트 공백을 조사하고 보고서/로그만 작성
- 사용 도구: `rg`, `sed`, `functions.get_goal`, `apply_patch`, `mkdir -p`
- 모델: GPT-5 Codex 세션
- reasoning effort: 높음
- 토큰 사용량: 도구에서 수치가 노출되지 않아 미기록(`get_goal` 결과 `null`)
