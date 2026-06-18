# Project-Wide Domain Integration Gaps - Cycle Summary

## Scope

- Workstream: `integration-test`
- Cycle: `project-wide-integration-gaps`
- Approved implementation scope:
  - PostgreSQL Testcontainers와 Redis Testcontainers를 사용한 통합 테스트 하네스 구성
  - 레슨 Redis Stream 신청 end-to-end 통합 테스트
  - 쿠폰 발급 Redis Stream end-to-end 통합 테스트
- Scope adjustment:
  - 사용자 추가 지시에 따라 쿠폰 도메인은 진행하지 않았다.
  - 쿠폰 대신 레슨 Redis Stream 대표 E2E와 JWT 필터 체인 smoke 통합 테스트를 구현했다.

## Implemented Tests

- `LessonRedisStreamIntegrationTest`
  - `LessonApplyProducer`로 신청을 enqueue한다.
  - `LessonAdmissionScheduler`를 수동 구동해 waiting room 요청을 Redis Stream으로 이동시킨다.
  - `LessonApplyConsumer`를 수동 구동해 DB `lesson_participants` 반영, status 기록, stream cleanup을 확인한다.
  - `LessonStockReconciliationScheduler`를 수동 구동해 `lessons.participant_count`와 Redis stock 동기화를 확인한다.
- `JwtAuthenticationFilterIntegrationTest`
  - 유효한 Bearer token으로 `/api/v1/users/me` 요청이 통과하는지 확인한다.
  - 일반 사용자 token으로 `/api/v1/admin/coupons` 접근이 거부되는지 확인한다.

## Infrastructure

- `PostgresIntegrationTestSupport`
  - PostgreSQL/PostGIS Testcontainers 기반 security 통합 테스트 support.
- `RedisStreamIntegrationTestSupport`
  - PostgreSQL/PostGIS + Redis Testcontainers 기반 Redis Stream 통합 테스트 support.
- `build.gradle`
  - Testcontainers JUnit Jupiter/PostgreSQL test dependency 추가.
  - Docker Desktop 29 환경에서 Testcontainers가 유효한 Docker API version을 사용하도록 test task 환경값 보강.
- `application-test.yml`
  - test profile의 datasource, Redis, mail, AWS, payment, JWT 기본값 일부 보강.

## Validation

```text
명령: ./gradlew --no-daemon test --tests '*IntegrationTest' --rerun-tasks
결과: BUILD SUCCESSFUL

실제 실행 해석:
- LessonRedisStreamIntegrationTest: tests=1, skipped=0, failures=0, errors=0
- JwtAuthenticationFilterIntegrationTest: tests=2, skipped=0, failures=0, errors=0

주의:
- Docker Desktop 29에서 낮은 Docker API version `/info` 응답이 비어 Testcontainers 연결이 실패했으므로, test task에서 `api.version=1.40`을 지정해 재검증했다.
- 테스트 종료 시점에 컨테이너가 내려간 뒤 Spring shutdown hook/Hikari가 DB 연결 경고를 남길 수 있으나, Gradle과 XML 결과 기준 신규 통합 테스트 3건은 실제 실행 통과했다.
```

## Review Result

- Reviewer 서브에이전트를 별도 실행했고, 최종 리뷰 리포트를 작성했다.
- 주요 리뷰 결론:
  - Docker 사용 가능 환경에서 skipped 없이 재검증했고 신규 통합 테스트 3건이 통과했다.
  - 쿠폰 Redis Stream E2E는 이번 cycle에서 제외되었고 남은 P1 공백이다.
  - 레슨 테스트는 대표 E2E 경로 검증이며, 운영 listener/scheduler 자동 wiring 전체 검증으로 설명하면 안 된다.
  - JWT 테스트는 최종 상태에서 Redis Stream 하네스와 분리되었다.

## Follow-up Candidates

- CI에서 통합 테스트가 전부 skipped 되는 경우 실패 처리하는 정책 검토.
- 레슨 Redis Stream의 중복 신청, 초과 신청, pending recovery, ACK/XDEL 실패 경로 추가.
- 쿠폰 Redis Stream E2E는 도메인 소유자와 별도 계획 수립 후 진행.
- `integrationTest` Gradle task 또는 CI matrix 분리.
