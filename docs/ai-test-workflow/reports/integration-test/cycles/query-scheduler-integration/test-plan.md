# P2 쿼리/스케줄러 통합 테스트 계획

## 실행 정보

- 역할: Planner
- 작업 시점: 2026-06-16
- 기준 문서: `docs/ai-test-workflow/AGENTS.md`, `docs/ai-test-workflow/rules/agent-roles.md`, `docs/ai-test-workflow/rules/human-approval-policy.md`, `docs/ai-test-workflow/rules/test-boundary.md`, `docs/ai-test-workflow/rules/enforcement-checklist.md`
- 입력 기준: `docs/ai-test-workflow/reports/integration-test/cycles/query-scheduler-integration/research-report.md`
- 이번 사이클 범위:
  1. PostgreSQL/PostGIS 레슨 검색 쿼리
  2. 레슨 재고 보정 스케줄러
  3. 프로필 메타데이터 스케줄러

## 승인 전 원칙

- 승인 전 구현 금지
- 운영 코드 수정 금지
- 성능 테스트를 회귀 테스트로 섞지 않기
- 승인 범위 밖 파일 수정 금지
- 단위 테스트는 이번 cycle의 주요 산출물로 삼지 않기
- `LessonSearchPerformanceTest`, `LocationSearchPerformanceTest`는 관측/벤치마크로만 유지하고 정합성 회귀에 포함하지 않기

## 후보별 계획

| 후보 | 현재 테스트 존재 여부 | 통합 테스트가 필요한 이유 | 단위 테스트와의 경계 | 이번 사이클 최소 승인 범위 | 필요한 인프라 / Testcontainers | 데이터 setup / cleanup | 후속으로 분리할 후보 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| PostgreSQL/PostGIS 레슨 검색 쿼리 | `StudentLessonServiceTest`의 mock 기반 분기 테스트만 있다. `LessonSearchPerformanceTest` / `LocationSearchPerformanceTest`는 존재하지만 정합성 회귀 테스트가 아니다. | native query, `ST_DWithin`, 거리 정렬, count/list 일치, 필터 조합이 실제 PostgreSQL/PostGIS에서만 검증된다. | 서비스의 sort 검증, keyword 유무 분기, DTO 매핑 호출 여부는 단위 테스트에 남긴다. repository native SQL 결과와 spatial 정합성은 통합 테스트로 넘긴다. | `LessonRepositoryIntegrationTest` 1개를 새로 추가해 keyword 검색 1개, location 검색 1개, count/list 일치 1개 정도의 대표 경로만 고정한다. | PostgreSQL Testcontainers + PostGIS. Redis는 필요 없다. 기존 `PostgresIntegrationTestSupport` 계열을 재사용하고, 새 인프라 설정은 만들지 않는다. | 최소 `User`, `Profile`, `Lesson`, `LessonImage`를 넣고, location 검색용 `location_point`와 keyword가 포함된 lesson name을 준비한다. cleanup은 repository delete 또는 테스트 롤백 우선으로 두고, 테스트마다 고유 식별자를 사용한다. | 검색 조건이 더 늘어나면 keyword/search-by-location을 별도 클래스나 케이스 묶음으로 분리한다. 성능 측정은 별도 벤치마크로 유지한다. |
| 레슨 재고 보정 스케줄러 | `LessonRedisStreamIntegrationTest`가 있지만 happy path 위주다. busy / stream lag / dirty set / 음수 재고 / 파싱 실패 분기가 비어 있다. | scheduler는 Redis와 PostgreSQL을 동시에 읽고 쓰며, dirty set 정리와 DB stock 동기화가 함께 맞아야 한다. mock만으로는 장애 분기를 고정하기 어렵다. | busy/lag/parse 실패 같은 의사결정 조건은 단위 테스트 후보가 될 수 있지만, Redis/Postgres 상태가 맞물리는 실제 회귀는 통합 테스트가 담당해야 한다. | 기존 happy path는 유지하고, `LessonStockReconciliationSchedulerIntegrationTest` 1개를 새로 추가해 busy skip, stream lag skip, dirty set 비어 있음, 음수 stock 또는 파싱 실패 같은 고위험 분기를 대표로 고정한다. | PostgreSQL + Redis Testcontainers. 기존 `RedisStreamIntegrationTestSupport` 계열을 재사용한다. 별도 로컬 Redis/Postgres 의존은 두지 않는다. | 최소 `User`, `Lesson`, `LessonParticipant`와 Redis의 dirty/busy/stock/last_active/waiting-room/stream 상태를 명시적으로 준비한다. cleanup은 DB repository delete와 Redis key/stream 정리로 고정하고, 테스트 간 상태 오염을 막기 위해 each-test cleanup을 둔다. | decision tree가 더 커지면 `LessonStockReconciliationSchedulerUnitTest`로 skip 조건을 분리하고, 통합 테스트는 DB/Redis 재동기화만 남긴다. |
| 프로필 메타데이터 스케줄러 | scheduler 자체 테스트는 없다. `ProfileMetadataServiceTest`만 존재한다. | scheduler가 사용자 목록을 순회하고, 리뷰 수/평점 집계를 DB 결과로 반영하는 경로는 실제 DB aggregate로 확인해야 한다. | 서비스의 null 처리, 저장 여부, user not found 같은 분기는 단위 테스트로 남긴다. scheduler 반복과 repository aggregate 결과는 통합 테스트로 검증한다. | `ProfileMetadataSchedulerIntegrationTest` 1개를 새로 추가해 사용자 순회 1개, 리뷰 집계 1개, 리뷰가 없을 때 기본값 처리 1개를 대표 경로로 고정한다. | PostgreSQL Testcontainers만 사용한다. Redis는 필요 없다. 기존 Postgres 통합 지원을 재사용한다. | 최소 `User`, `ProfileMetadata`, `Review`와 리뷰 작성자/대상자 데이터를 준비한다. cleanup은 repository delete 또는 rollback 우선으로 두고, 평균 평점과 review count가 고정되도록 fixture를 단순하게 유지한다. | scheduler 순회와 aggregate 조회가 더 분리되면 `ProfileMetadataSchedulerUnitTest`와 `ProfileMetadataRepositoryIntegrationTest`로 나눈다. |

## 이번 사이클에서 승인받을 최소 구현 범위

1. `src/test/java/com/threestar/trainus/domain/lesson/teacher/repository/LessonRepositoryIntegrationTest.java` 신규 추가
2. `src/test/java/com/threestar/trainus/domain/lesson/issue/LessonStockReconciliationSchedulerIntegrationTest.java` 신규 추가
3. `src/test/java/com/threestar/trainus/domain/metadata/scheduler/ProfileMetadataSchedulerIntegrationTest.java` 신규 추가

이번 승인 범위는 위 3개 통합 테스트 파일로 한정한다. `src/main/java/**` 운영 코드, 성능 테스트, 공통 설정 파일, build 설정은 이번 사이클에서 수정하지 않는다.

## 수정 허용 파일

- `src/test/java/com/threestar/trainus/domain/lesson/teacher/repository/LessonRepositoryIntegrationTest.java`
- `src/test/java/com/threestar/trainus/domain/lesson/issue/LessonStockReconciliationSchedulerIntegrationTest.java`
- `src/test/java/com/threestar/trainus/domain/metadata/scheduler/ProfileMetadataSchedulerIntegrationTest.java`
- `docs/ai-test-workflow/reports/integration-test/cycles/query-scheduler-integration/test-plan.md`
- `docs/ai-test-workflow/logs/integration-test/cycles/query-scheduler-integration/prompt-and-tool-log.md`

## 수정 금지 파일

- `src/main/java/**`
- `src/main/resources/**`
- `src/test/java/**/*UnitTest.java`
- `src/test/java/**/*PerformanceTest.java`
- `src/test/java/com/threestar/trainus/domain/lesson/student/LessonSearchPerformanceTest.java`
- `src/test/java/com/threestar/trainus/domain/lesson/student/LocationSearchPerformanceTest.java`
- `src/test/resources/application-test.yml`
- `build.gradle`
- `.github/**`
- `docs/**` 전체 중 이번 cycle의 두 기록 파일 외 나머지

## 검증 명령 후보

- `./gradlew test --tests 'com.threestar.trainus.domain.lesson.teacher.repository.LessonRepositoryIntegrationTest'`
- `./gradlew test --tests 'com.threestar.trainus.domain.lesson.issue.LessonStockReconciliationSchedulerIntegrationTest'`
- `./gradlew test --tests 'com.threestar.trainus.domain.metadata.scheduler.ProfileMetadataSchedulerIntegrationTest'`
- `./gradlew test --tests '*IntegrationTest'`

## 후속으로 분리할 후보

- 검색 쿼리의 keyword/location 케이스가 늘어나면 repository 정합성 클래스와 spatial 정합성 클래스를 분리한다.
- 재고 보정 스케줄러의 decision tree가 커지면 skip 조건은 단위 테스트로, Redis/Postgres 동기화는 통합 테스트로 분리한다.
- 프로필 메타데이터는 scheduler 순회와 aggregate 조회가 섞이면 scheduler unit test와 repository integration test로 나눈다.
- `LessonSearchPerformanceTest`와 `LocationSearchPerformanceTest`는 계속 벤치마크로만 유지한다.

## 승인 요청 요약

- 목적: PostgreSQL/PostGIS 검색, Redis/PostgreSQL 재고 보정, 프로필 메타데이터 집계의 실제 회귀 경로를 좁게 고정한다.
- 외부 의존성: PostgreSQL Testcontainers, Redis Testcontainers(재고 보정만)
- 하지 않을 것: 운영 코드 수정, 성능 테스트 회귀화, 승인 범위 밖 파일 수정, 구현 전 검증 실행, PR/push
- 승인 판단 기준: 현재 테스트 공백이 크고, 새 통합 테스트는 각 후보당 1개 파일로 제한되어 있어 승인 가능한 최소 범위다.
