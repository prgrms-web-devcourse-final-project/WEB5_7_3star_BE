# Project-Wide Domain Integration Gaps - Review Report

## 실행 정보

```text
실행 일시: 2026-06-15
에이전트 역할: Main agent review update
작업 묶음: integration-test
작업 주제: project-wide-integration-gaps
검토 범위: 구현된 통합 테스트, Testcontainers 하네스, test profile 변경, Gradle 테스트 의존성, 검증 결과 해석
수정 범위: 이 review-report.md만 최종 검증 결과 기준으로 갱신
수정하지 않은 파일: 운영 코드, 테스트 코드, 운영 리소스
```

## 종합 판단

이번 구현은 PostgreSQL/PostGIS와 Redis를 Testcontainers로 묶는 테스트 하네스를 추가하고, 레슨 Redis Stream 신청 대표 흐름과 JWT 필터 체인 smoke 경로를 `*IntegrationTest` 명명 규칙 아래 분리했다.

최종 구현 범위는 PostgreSQL/Redis Testcontainers 하네스, 레슨 Redis Stream 신청 대표 E2E 통합 테스트, JWT 필터 체인 smoke 통합 테스트다. 쿠폰 도메인은 사용자 지시에 따라 진행하지 않았고, 쿠폰 Redis Stream E2E 검증은 이번 cycle의 완료 항목이 아니다.

최종 검증은 Docker Desktop 실행 환경에서 `./gradlew --no-daemon test --tests '*IntegrationTest' --rerun-tasks`로 수행했고, 신규 통합 테스트 3건이 skipped 없이 통과했다. Docker Desktop 29 환경에서는 낮은 Docker API version `/info` 응답이 비어 Testcontainers 연결이 실패했기 때문에, test task에 `api.version=1.40`과 `DOCKER_API_VERSION=1.40`을 지정해 재검증했다.

## 심각도별 문제 목록

### High

없음.

최종 XML 결과 기준으로 `LessonRedisStreamIntegrationTest` 1건과 `JwtAuthenticationFilterIntegrationTest` 2건은 모두 `skipped=0`, `failures=0`, `errors=0`이다.

### Medium

1. 레슨 테스트는 대표 E2E 경로 검증이며, 운영의 비동기/스케줄링 wiring 전체 검증은 아니다.

- 근거: `LessonRedisStreamIntegrationTest`는 `LessonAdmissionScheduler`, `LessonApplyConsumer`, `LessonStockReconciliationScheduler`를 직접 생성하고, consumer의 private buffer 처리를 `ReflectionTestUtils.invokeMethod`로 호출한다.
- 영향: producer, Redis Stream, admission, consumer service 처리, DB 반영, status 기록, stream cleanup, stock reconciliation의 대표 흐름은 검증한다. 그러나 Spring-managed scheduler 등록, listener container 자동 소비, scheduling trigger는 범위 밖이다.
- 권장: 외부 설명은 "레슨 Redis Stream 신청의 대표 E2E 경로를 실제 Redis/PostgreSQL 기반으로 검증" 수준으로 제한한다.

2. 쿠폰 P1 후보는 명시적으로 남은 공백이다.

- 근거: 사용자 추가 지시에 따라 쿠폰 도메인 통합 테스트 구현을 중단했고, 최종 변경 파일에도 쿠폰 테스트는 없다.
- 영향: 쿠폰 재고 차감, Stream enqueue, consumer ACK/pending, rollback 경로는 이번 cycle 결과물로 해소되지 않았다.
- 권장: PR draft와 cycle summary에서 쿠폰은 "보류/제외"로 표시하고, 구현 완료 항목에 넣지 않는다.

3. `application-test.yml` 변경은 신규 `integration-test` profile 실행의 핵심 근거가 아니다.

- 근거: 신규 공통 하네스는 `@ActiveProfiles("integration-test")`를 사용하고, 핵심 datasource/Redis/JWT property는 `@DynamicPropertySource`가 주입한다.
- 영향: `application-test.yml`의 default 값 보강은 일반 test profile에는 유효할 수 있으나, 이번 신규 테스트의 실행 독립성을 보장하는 주된 장치는 아니다.
- 권장: 문서에서는 하네스의 `DynamicPropertySource`가 컨테이너 주소와 테스트 기본값을 주입한다고 설명한다.

### Low

1. 통합 테스트 전용 Gradle task는 아직 없다.

- 근거: `build.gradle`은 Testcontainers 의존성과 test task 환경값을 추가했지만, 별도 `integrationTest` task 분리는 없다.
- 영향: 현재는 `./gradlew test --tests '*IntegrationTest'`로 선별 실행해야 한다.
- 권장: 후속 cycle에서 `integrationTest` task 또는 CI matrix 분리를 검토한다.

2. Redis cleanup은 전용 컨테이너 전제에서는 허용 가능하지만 범위가 넓다.

- 근거: `RedisStreamIntegrationTestSupport`가 `StringRedisTemplate.keys("*")` 후 전체 delete를 수행한다.
- 영향: Testcontainers 전용 Redis에서는 격리에 효과적이다. 설정 오류로 로컬 Redis를 바라보는 상황에서는 위험하므로, 전용 테스트 컨테이너 전제로 유지해야 한다.

## 검증 결과 해석

```text
실행 명령: ./gradlew --no-daemon test --tests '*IntegrationTest' --rerun-tasks
관찰 결과: BUILD SUCCESSFUL

XML 결과:
- LessonRedisStreamIntegrationTest: tests=1, skipped=0, failures=0, errors=0
- JwtAuthenticationFilterIntegrationTest: tests=2, skipped=0, failures=0, errors=0
```

테스트 종료 시점에 컨테이너가 내려간 뒤 Spring shutdown hook/Hikari가 DB 연결 경고를 남겼지만, Gradle exit code와 XML 결과는 성공이다.

## 아직 주장하면 안 되는 내용

- "쿠폰 Redis Stream E2E 테스트를 추가했다"라고 주장하면 안 된다. 쿠폰 범위는 제외되었고 구현도 없다.
- "운영 비동기 consumer/listener/scheduler 전체 wiring을 검증했다"라고 주장하면 안 된다. 레슨 테스트는 일부 컴포넌트를 직접 생성하고 private method를 반사 호출한다.
- "단위/통합 테스트 실행 경계가 Gradle task로 분리됐다"라고 주장하면 안 된다. 현재는 naming과 선별 실행 명령 중심이다.

## Reviewer 결론

레슨 Redis Stream 대표 경로와 JWT filter chain smoke 경로는 실제 PostgreSQL/PostGIS 및 Redis Testcontainers 환경에서 skipped 없이 통과했다. 쿠폰 통합 테스트는 사용자 지시에 따라 제외되었고 남은 P1 공백이다. 남은 주요 리스크는 자동 listener/scheduler wiring 미검증, 쿠폰 도메인 미검증, 통합 테스트 전용 Gradle task/CI 정책 부재다.
