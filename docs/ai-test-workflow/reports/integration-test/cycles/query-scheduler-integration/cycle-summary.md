# P2 쿼리/스케줄러 통합 테스트 사이클 요약

## 범위

- 레슨 도메인 통합 테스트만 유지한다.
- 쿠폰 도메인은 이번 사이클에서 제외한다.
- 공통 테스트 하네스와 설정 변경이 다른 테스트에 미치는 영향은 별도로 확인한다.

## 구현 결과

- `LessonRepositoryIntegrationTest`
  - PostgreSQL/PostGIS 기반 레슨 검색 대표 경로를 검증한다.
  - 주소 검색, 키워드 검색, 위치 검색에서 count와 list 정합성을 확인한다.
- `LessonStockReconciliationSchedulerIntegrationTest`
  - Redis Stream, waiting room, dirty set, 재고 동기화를 검증한다.
  - 정상 보정, waiting room 잔여, 음수 재고, stream backlog, busy key 보류를 분리해 확인한다.
- `LessonRedisStreamIntegrationTest`
  - 레슨 신청 enqueue/admission 흐름의 대표 경로를 검증한다.
- `LessonRedisStreamFailureIntegrationTest`
  - 중복 신청 거부, 재고 부족 거부, status 조회 실패 재등록, pipeline 실패 재등록을 검증한다.

## 이전 테스트 영향 확인

- 개별 실행 기준으로 다음 테스트는 모두 성공했다.
  - `LessonRepositoryIntegrationTest`
  - `LessonStockReconciliationSchedulerIntegrationTest`
  - `LessonRedisStreamIntegrationTest`
  - `LessonRedisStreamFailureIntegrationTest`
  - `JwtAuthenticationFilterIntegrationTest`
- 병렬로 여러 `SpringBootTest`를 한 번에 돌리면 Testcontainers와 `build/test-results`가 서로 간섭해서 실패할 수 있었다.
- 따라서 범위 영향 판정은 병렬 결과가 아니라 개별 실행 결과로 판단해야 한다.

## 범위 밖 변경

- 현재 작업트리에는 `build.gradle`과 `src/test/resources/application-test.yml` 변경이 남아 있다.
- 이 두 파일은 레슨 테스트만이 아니라 전체 통합 테스트 실행 환경에 영향을 준다.
- 레슨-only 사이클을 엄격하게 유지하려면 이 변경은 별도 승인 범위로 분리해야 한다.

## 결론

- 레슨 도메인 테스트 자체는 개별 실행 기준으로 유지된다.
- 다만 공통 설정 변경은 이전 테스트 전반에 영향을 줄 수 있다.
- 현재 상태를 그대로 PR 설명에 넣을 때는, 레슨 테스트 성공과 공통 설정 변경의 범위를 분리해서 적는 것이 맞다.
