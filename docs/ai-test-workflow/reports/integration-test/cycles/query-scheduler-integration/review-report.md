# P2 쿼리/스케줄러 통합 테스트 리뷰 보고서

## Findings

### 높음 - 레슨 도메인만 다룬다는 검토 기준을 충족하지 못함

- 대상 파일: `src/test/java/com/threestar/trainus/domain/metadata/scheduler/ProfileMetadataSchedulerIntegrationTest.java`
- 근거:
  - 테스트 클래스가 `domain/metadata/scheduler`에 추가되어 있고, `ProfileMetadataScheduler`, `ProfileMetadataRepository`, `ReviewRepository`, `UserRepository`를 직접 사용한다(`ProfileMetadataSchedulerIntegrationTest.java:1-25`, `31-40`).
  - fixture도 `ProfileMetadata`, `Review`, `User` 집계를 검증하며(`ProfileMetadataSchedulerIntegrationTest.java:53-67`, `79-115`), 레슨은 리뷰 생성용 연결 데이터로만 사용된다(`87-105`).
- 영향:
  - 이번 리뷰 기준의 "레슨 도메인만 다뤘는지"에는 맞지 않는다.
  - 테스트 자체는 실제 PostgreSQL 집계를 확인하지만, 산출물을 레슨 도메인 통합 테스트 사이클 완료 항목으로 주장하면 범위가 과장된다.
- 권고:
  - 레슨-only 사이클에서는 이 파일을 제외하거나, 별도 metadata/review 사이클 산출물로 분리해야 한다.

### 높음 - 승인 범위 밖 파일 변경이 포함됨

- 대상 파일:
  - `src/test/java/com/threestar/trainus/testsupport/PostgresIntegrationTestSupport.java`
  - `src/test/java/com/threestar/trainus/testsupport/RedisStreamIntegrationTestSupport.java`
  - 현재 작업트리 기준 추가 변경 흔적: `build.gradle`, `src/test/resources/application-test.yml`
- 근거:
  - 이번 cycle의 `test-plan.md`는 최소 구현 범위를 3개 통합 테스트 파일로 제한했다(`docs/ai-test-workflow/reports/integration-test/cycles/query-scheduler-integration/test-plan.md:31-37`).
  - 수정 허용 파일 목록에도 공통 test support는 없다(`test-plan.md:39-46`).
  - 수정 금지 파일에는 `build.gradle`과 `src/test/resources/application-test.yml`이 명시되어 있다(`test-plan.md:47-58`).
  - 그런데 검토 대상에는 공통 하네스 2개가 포함되어 있고, `PostgresIntegrationTestSupport`와 `RedisStreamIntegrationTestSupport`는 datasource/Redis/JWT/스케줄링 property 및 Testcontainers 초기화를 제공한다(`PostgresIntegrationTestSupport.java:17-69`, `RedisStreamIntegrationTestSupport.java:22-90`).
- 영향:
  - 공통 하네스 변경은 이번 3개 테스트 파일을 넘어 다른 통합 테스트의 실행 환경에도 영향을 준다.
  - `build.gradle`, `application-test.yml` 변경이 이번 cycle 변경이라면 승인 범위 위반이다. 이번 리뷰에서는 사용자가 지정한 상세 검토 대상이 아니므로 내용 검토는 하지 않았다.
- 권고:
  - 공통 하네스와 빌드/test profile 변경은 별도 승인 항목으로 기록하거나, 이번 cycle 완료 주장에서는 제외해야 한다.

### 보통 - 재고 보정 스케줄러의 stream lag/busy 회귀가 실제 Redis 상태로 고정되지 않음

- 대상 파일: `src/test/java/com/threestar/trainus/domain/lesson/issue/LessonStockReconciliationSchedulerIntegrationTest.java`
- 근거:
  - 운영 스케줄러는 waiting room, Redis Stream pending/backlog, busy key, 음수/파싱 불가 stock을 모두 보정 연기 조건으로 본다(`LessonStockReconciliationScheduler.java:41-52`, `72-117`, `178-229`).
  - 현재 테스트는 정상 보정, waiting room 잔여, 음수 stock만 검증한다(`LessonStockReconciliationSchedulerIntegrationTest.java:54-128`).
  - `prepareEmptyStreamGroup()`는 seed stream을 만들고 consumer group 생성 뒤 trim하여 stream을 비우는 보조 로직이다(`LessonStockReconciliationSchedulerIntegrationTest.java:130-149`). 따라서 실제 stream backlog 또는 pending message가 있을 때 dirty set/DB/Redis stock이 보존되는 경로는 검증하지 않는다.
- 영향:
  - `hasStreamLag()`의 consumer group pending count 및 stream size 조건이 깨져도 현재 테스트 묶음은 통과할 수 있다.
  - busy key의 30초 safety margin, stale reset, 음수 busy count 처리도 이번 통합 테스트에서 회귀로 잡히지 않는다.
- 권고:
  - 최소한 stream entry가 남아 있는 경우와 consumer group pending이 있는 경우를 실제 Redis Stream 상태로 추가 고정해야 한다.
  - busy decision tree 전체를 통합 테스트로 늘리기 어렵다면, busy 분기는 단위 테스트로 분리하고 통합 테스트는 DB/Redis 동기화와 stream lag gate만 좁게 보강하는 편이 낫다.

### 낮음 - repository 검색 테스트는 대표 경로만 검증하며 필터 경계가 남아 있음

- 대상 파일: `src/test/java/com/threestar/trainus/domain/lesson/teacher/repository/LessonRepositoryIntegrationTest.java`
- 근거:
  - 주소 검색은 `ri`를 빈 문자열로만 넘긴다(`LessonRepositoryIntegrationTest.java:38-75`). 운영 쿼리는 `:ri IS NULL OR l.ri = :ri` 조건을 사용한다(`LessonRepository.java:372-377`).
  - 위치 검색은 keyword 없는 공간 검색 1개만 검증한다(`LessonRepositoryIntegrationTest.java:112-142`). 위치 기반 keyword 검색이나 null category/null region 조합은 이번 테스트에 없다.
- 영향:
  - 현재 3개 테스트는 count/list 일치, 가격 정렬, PostGIS 거리 필터의 핵심 대표 경로는 실제 PostgreSQL/PostGIS로 검증한다.
  - 다만 `ri = null`과 `ri = ""` 차이, null 필터 조합 같은 회귀는 아직 남아 있다.
- 권고:
  - 이번 cycle 완료 설명은 "대표 경로 검증"으로 제한하고, 필터 조합 확대는 후속으로 분리한다.

## 열린 질문 / 가정

- `build.gradle`과 `src/test/resources/application-test.yml` 변경이 이번 P2 cycle 변경인지, 이전 cycle의 잔여 변경인지 확인이 필요하다. 이번 cycle 변경이라면 수정 금지 파일 변경이다.
- `ProfileMetadataSchedulerIntegrationTest`는 `test-plan.md`에는 포함되어 있었지만, 사용자의 현재 리뷰 기준인 "레슨 도메인만"과는 충돌한다고 보았다.
- 사용자가 제공한 개별 Gradle 실행 성공 결과는 신뢰했다. 이번 리뷰에서는 테스트를 재실행하지 않았다.

## 요약

`LessonRepositoryIntegrationTest`는 실제 PostgreSQL/PostGIS 대표 검색 경로를 검증하고, `LessonStockReconciliationSchedulerIntegrationTest`도 DB/Redis 보정의 일부 핵심 경로를 검증한다. 그러나 이번 기준대로라면 metadata 스케줄러 테스트는 레슨-only 범위를 벗어나며, 공통 test support와 설정 변경은 승인 범위 밖 변경으로 보인다. 재고 보정은 stream lag/pending 및 busy gate 회귀가 아직 고정되지 않아 후속 보강이 필요하다.
