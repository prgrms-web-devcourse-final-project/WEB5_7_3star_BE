# Project-Wide Domain Integration Gaps - Test Plan

## 실행 정보

```text
실행 일시: 2026-06-15 17:06:14 KST
사용 모델: gpt-5.4-mini
에이전트 역할: Planner
사용 프롬프트: docs/ai-test-workflow/prompts/planner.md
참조 문서: docs/ai-test-workflow/AGENTS.md, docs/ai-test-workflow/rules/agent-roles.md, docs/ai-test-workflow/rules/human-approval-policy.md, docs/ai-test-workflow/rules/test-boundary.md, docs/ai-test-workflow/rules/enforcement-checklist.md, docs/ai-test-workflow/templates/test-plan-template.md
참조한 Research Report: docs/ai-test-workflow/reports/integration-test/cycles/project-wide-integration-gaps/research-report.md
```

## 작업 목표

```text
이번 작업의 목표:
- Researcher가 확인한 프로젝트 전체 도메인의 통합 테스트 공백을 P1/P2/P3로 분류하고, 승인 가능한 최소 통합 테스트 보강 범위를 제안한다.
- Redis, PostgreSQL, Scheduler, Consumer, Security의 실제 연결 경로를 우선하고, 단위 테스트는 이번 사이클에서 추가하지 않는다.
- Testcontainers와 테스트 fixture/cleanup 전략을 포함해 구현 전 승인 요청에 필요한 정보를 문서화한다.

이번 작업에서 하지 않을 것:
- 운영 코드 수정
- 테스트 코드 생성/수정
- 설정 변경
- 구현
- 검증 실행
- PR 생성
- push
- 단위 테스트 추가
```

## 통합 테스트 후보 목록

| 우선순위 | 후보 | 테스트 유형 | 목적 | 비고 |
| --- | --- | --- | --- | --- |
| P1 | 레슨 Redis Stream 신청 end-to-end | `*IntegrationTest` | producer -> consumer -> admission scheduler -> pending recovery -> stock reconciliation의 실제 연결 경로를 고정한다. | 이번 사이클의 최우선 승인 대상 |
| P1 | 쿠폰 발급 Redis Stream end-to-end | `*IntegrationTest` | 재고 차감, 메시지 enqueue, consumer ACK/pending 처리, 상태 갱신을 실제 Redis/DB로 검증한다. | 레슨 경로와 동일한 공통 harness 사용 |
| P1 | JWT 보안 필터 체인 smoke 통합 테스트 | `*IntegrationTest` | 인증 실패/성공, 관리자 권한, filter chain 통과 여부를 실제 HTTP 경로로 확인한다. | 별도 HTTP fixture 필요 |
| P2 | PostgreSQL/PostGIS 레슨 검색 쿼리 | `*IntegrationTest` | 실제 쿼리 결과, 페이징, count, location 검색의 correctness를 검증한다. | 성능 테스트와 분리 |
| P2 | 레슨 재고 보정 스케줄러 | `*IntegrationTest` | DB count와 Redis stock을 맞추는 배치/락 경로를 검증한다. | ShedLock 포함 |
| P2 | 프로필 메타데이터 스케줄러 | `*IntegrationTest` | 리뷰 집계와 프로필 메타데이터 반영을 검증한다. | DB side effect 중심 |
| P3 | 댓글/리뷰 HTTP 인증 흐름 | `*IntegrationTest` | 세션 주입이 아닌 JWT 기반 인증/인가 흐름을 고정한다. | P1 보안 경로와 중복 가능성 있음 |
| P3 | 결제/S3 경계 후보 | 별도 후보 | 외부 API와 파일 업로드 경계가 실제로 필요한 경우에만 분리한다. | 이번 사이클 범위 밖 |

## 단위 테스트와의 경계

```text
이번 사이클에서는 단위 테스트를 새로 추가하지 않는다.

단위 테스트로 남길 영역:
- DTO 검증
- mapper 변환
- entity invariant
- 외부 인프라 없이 끝나는 서비스 분기와 예외 처리

통합 테스트로 넘길 영역:
- Spring context가 필요한 흐름
- Redis/PostgreSQL 실제 연결 경로
- scheduler, consumer, transaction, security filter chain

경계 원칙:
- 통합 테스트가 DTO/mapper 단위의 세부 분기까지 다시 검증하지 않는다.
- 단위 테스트가 이미 충분한 영역은 통합 테스트에서 대표 경로 1개만 확인한다.
```

## 테스트 보강 우선순위

| 순서 | 대상 | 테스트 유형 | 목적 | 예상 변경 파일 |
| --- | --- | --- | --- | --- |
| 1 | 레슨 Redis Stream 신청 end-to-end | 통합 테스트 | 가장 높은 회귀 위험을 가진 core flow를 먼저 고정한다. | `src/test/java/com/threestar/trainus/domain/lesson/**/**/*IntegrationTest.java`, `src/test/java/com/threestar/trainus/testsupport/**` |
| 2 | 쿠폰 발급 Redis Stream end-to-end | 통합 테스트 | 동일한 Redis/DB harness 위에서 두 번째 핵심 도메인 흐름을 검증한다. | `src/test/java/com/threestar/trainus/domain/coupon/**/**/*IntegrationTest.java`, `src/test/java/com/threestar/trainus/testsupport/**` |
| 3 | JWT 보안 필터 체인 smoke | 통합 테스트 | 인증/인가 경로의 실제 filter chain 동작을 확인한다. | `src/test/java/com/threestar/trainus/global/config/security/**/**/*IntegrationTest.java`, `src/test/java/com/threestar/trainus/domain/**/controller/**` |
| 4 | PostgreSQL/PostGIS 검색 쿼리 | 통합 테스트 | 실제 DB 쿼리 correctness를 확보한다. | `src/test/java/com/threestar/trainus/domain/lesson/**/repository/**IntegrationTest.java` |
| 5 | 배치/스케줄러 경로 | 통합 테스트 | transactional side effect와 락/재처리를 검증한다. | `src/test/java/com/threestar/trainus/domain/**/scheduler/**IntegrationTest.java` |

## 통합 테스트 계획

```text
대상:
- 레슨 Redis Stream 신청 end-to-end
- 쿠폰 발급 Redis Stream end-to-end
- JWT 보안 필터 체인 smoke 통합 테스트

필요 인프라:
- PostgreSQL Testcontainer
- Redis Testcontainer
- Spring Boot test context
- HTTP test client(MockMvc 또는 WebTestClient 중 기존 프로젝트 관행에 맞는 방식)
- 필요 시 Redis key namespace 분리와 fixed test data builder

Testcontainers 적용 여부:
- 적용한다.
- PostgreSQL과 Redis는 실제 외부 의존성 동작 자체가 검증 대상이므로 Testcontainers 우선이 적합하다.
- H2, mock Redis, 로컬 수동 인프라 의존은 이번 승인 범위에서 배제한다.

데이터 setup:
- 각 테스트는 고유한 lesson/coupon/user 식별자를 사용한다.
- DB seed는 테스트 메서드 단위로 최소 데이터만 삽입한다.
- Redis stream 및 pending 상태는 테스트 시작 시 명시적으로 초기화한다.
- 공통 fixture는 `testsupport` 패키지의 builder/helper로만 제한한다.

데이터 cleanup:
- 가능하면 트랜잭션 롤백을 기본으로 사용한다.
- 롤백으로 정리되지 않는 Redis 키/stream은 테스트 후 명시적으로 삭제한다.
- DB는 필요한 경우 테이블 단위 truncate 또는 repository cleanup을 사용한다.
- 테스트 간 상태 오염을 막기 위해 cleanup은 helper로 고정한다.
```

## 필요한 인프라

- Docker 기반 Testcontainers 실행 환경
- PostgreSQL 1개
- Redis 1개
- Spring Boot `test` profile의 독립 실행성
- Redis core/mq 경로를 구분할 수 있는 테스트용 prefix 또는 namespace
- 필요 시 Shared test fixture/support class

## 외부 의존성/Testcontainers 사용 여부

```text
사용 여부: 사용

대상:
- PostgreSQL
- Redis

판단:
- 실제 Redis Stream, consumer ACK/pending 처리, PostGIS/쿼리 correctness, scheduler side effect를 검증하려면 Testcontainers가 가장 적합하다.
- 이번 cycle에서는 외부 로컬 서비스 고정 의존을 승인 범위에 넣지 않는다.
```

## 데이터 setup / cleanup 전략

```text
setup:
- 테스트마다 독립적인 식별자와 최소 fixture만 생성한다.
- 공통적으로 필요한 user/lesson/coupon 데이터는 builder 기반으로 만든다.
- scheduler/consumer 테스트는 메시지와 상태를 분리된 step으로 준비한다.

cleanup:
- 테스트 종료 시 Redis key/stream/pending entry를 제거한다.
- DB는 트랜잭션 롤백 우선, 필요 시 truncate 보조를 사용한다.
- shared fixture는 테스트 클래스 간 상태를 보존하지 않는다.
```

## 제외 범위

```text
- 운영 코드 수정
- 단위 테스트 추가 또는 수정
- 설정 파일의 광범위한 재구성
- CI 파이프라인 변경
- 성능 테스트 리라이트
- 주석 처리된 기존 테스트의 복구
- API 응답 형식 변경
- DB schema 변경
- PR 생성 및 push
- 구현 전 검증 실행
```

## 승인 요청이 필요한 이유

```text
이 작업은 Redis Stream, PostgreSQL, scheduler, security filter chain처럼 여러 인프라 계층을 동시에 건드리는 통합 경로를 포함한다.
Testcontainers 도입 여부와 테스트 profile 격리 방식이 구현 난이도와 실행 환경에 직접 영향을 주므로, 승인 전 범위를 먼저 고정해야 한다.
또한 새 통합 테스트 파일, 공통 fixture, 필요 시 build/test 설정 변경이 발생할 수 있어 파일 수정 경계에 대한 사전 승인이 필요하다.
```

## 우선순위와 단계별 실행 순서

```text
1. P1 공통 harness를 전제로 레슨 Redis Stream 신청 end-to-end를 먼저 고정한다.
2. 같은 harness 위에서 쿠폰 발급 Redis Stream end-to-end를 추가한다.
3. 승인 범위가 확장되면 JWT 보안 필터 체인 smoke 통합 테스트를 별도 묶음으로 진행한다.
4. 이후 P2 후보인 PostgreSQL/PostGIS 검색 쿼리와 스케줄러 경로를 분리해 진행한다.
5. P3 후보는 이번 cycle의 승인 대상이 아니며 후속 후보로만 유지한다.
```

## 승인 가능한 최소 구현 범위

```text
이번 cycle에서 승인받을 최소 구현 범위는 다음 두 개다.
- 레슨 Redis Stream 신청 end-to-end
- 쿠폰 발급 Redis Stream end-to-end

이 최소 범위는 동일한 Redis/PostgreSQL Testcontainers harness를 재사용할 수 있어 승인 대비 효율이 높다.
JWT 보안 필터 체인 smoke 통합 테스트는 중요하지만 별도 HTTP fixture가 추가로 필요하므로, 이번 최소 승인 묶음에서는 분리하는 편이 낫다.
```

## 수정 허용 파일 또는 파일 패턴

```text
- src/test/java/com/threestar/trainus/domain/lesson/**/**/*IntegrationTest.java
- src/test/java/com/threestar/trainus/domain/coupon/**/**/*IntegrationTest.java
- src/test/java/com/threestar/trainus/global/config/security/**/**/*IntegrationTest.java
- src/test/java/com/threestar/trainus/domain/**/repository/**IntegrationTest.java
- src/test/java/com/threestar/trainus/domain/**/scheduler/**IntegrationTest.java
- src/test/java/com/threestar/trainus/testsupport/**
- src/test/resources/application-test.yml
- build.gradle
```

## 수정 금지 파일 또는 파일 패턴

```text
- src/main/java/**
- src/main/resources/**
- src/test/java/**/*UnitTest.java
- src/test/java/**/*PerformanceTest.java
- src/test/java/**/*Tests.java
- docs/** (이번 cycle의 계획/로그 파일 제외)
- .github/**
- 운영 코드, DB schema, API 계약에 해당하는 파일 전체
```

## 검증 명령 후보

```text
- ./gradlew test --tests '*IntegrationTest'
- ./gradlew test --tests 'com.threestar.trainus.domain.lesson.*IntegrationTest'
- ./gradlew test --tests 'com.threestar.trainus.domain.coupon.*IntegrationTest'
- ./gradlew integrationTest
```

## 승인 요청 요약

```text
승인 요청에 포함할 핵심:
- 목적: core domain과 infra dependency path의 통합 회귀를 고정한다.
- 범위: 레슨 Redis Stream E2E, 쿠폰 Redis Stream E2E, 필요 시 이후 JWT security smoke.
- 외부 의존성: PostgreSQL Testcontainer, Redis Testcontainer.
- 하지 않을 것: 운영 코드 수정, unit test 추가, 설정 광역 변경, 검증 실행, PR/push.
```
