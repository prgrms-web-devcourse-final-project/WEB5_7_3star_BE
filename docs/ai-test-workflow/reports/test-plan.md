# Test Plan

## 실행 정보

```text
실행 일시: 2026-06-11
사용 모델: GPT-5 Codex
에이전트 역할: Planner
사용 프롬프트: Researcher 결과를 바탕으로 TrainUs 테스트 보강 계획을 세우고, 파일 수정 없이 Planner 리포트와 로그 항목만 작성한다.
참조 문서:
- docs/ai-test-workflow/rules/agent-roles.md
- docs/ai-test-workflow/rules/human-approval-policy.md
- docs/ai-test-workflow/rules/test-boundary.md
- docs/ai-test-workflow/reports/research-report.md
```

## 작업 목표

```text
이번 작업의 목표:
- 5시간 안에 가능한 테스트 보강 범위를 정의한다.
- 단위 테스트와 통합 테스트 경계를 분리한다.
- Testcontainers 적용 여부를 명확히 판단한다.
- Human approval이 필요한 항목과 바로 진행 가능한 항목을 분리한다.
- 포트폴리오에 주장 가능한 결과와 아직 주장하면 안 되는 내용을 구분한다.

이번 작업에서 하지 않을 것:
- 운영 코드 수정
- 테스트 코드 생성
- build.gradle / test profile 수정 전제의 계획 확정
- 전체 테스트 완성 같은 과도한 목표 설정
```

## 테스트 보강 우선순위

| 순서 | 대상 | 테스트 유형 | 목적 | 예상 변경 파일 |
| --- | --- | --- | --- | --- |
| 1 | `LoginUserArgumentResolver` | 단위 테스트 | `@LoginUser Long` 허용 조건과 인증 실패 분기 고정 | `src/test/java/com/threestar/trainus/global/resolver/LoginUserArgumentResolverUnitTest.java` |
| 2 | `UserController`의 `/me`, `/withdraw` | 통합 테스트 | `@LoginUser` wiring과 현재 사용자 조회/탈퇴 경로 확인 | `src/test/java/com/threestar/trainus/domain/user/controller/UserControllerIntegrationTest.java` |
| 3 | `LessonWaitingRoomService` + `LessonAdmissionScheduler` | 통합 테스트 | Redis 대기열, admission batch, 실패 시 재큐잉 경로 고정 | `src/test/java/com/threestar/trainus/domain/lesson/issue/LessonAdmissionSchedulerIntegrationTest.java` |
| 4 | `LessonStockReconciliationScheduler` | 통합 테스트 | waiting room/stream lag/busy state 조건에서 보정 연기와 동기화 검증 | `src/test/java/com/threestar/trainus/domain/lesson/issue/LessonStockReconciliationSchedulerIntegrationTest.java` |
| 5 | 지역/위치 검색 회귀 | 통합 테스트 | 필터 조합과 페이지 결과 정확성 확인 | `src/test/java/com/threestar/trainus/domain/lesson/student/LessonSearchRepositoryIntegrationTest.java` |

## 단위 테스트 계획

```text
대상:
- LoginUserArgumentResolver

검증할 동작:
- `supportsParameter`가 `@LoginUser` + `Long` 조합만 허용하는지 확인
- `resolveArgument`가 authentication null / unauthenticated / principal 타입 불일치에서 `AUTHENTICATION_REQUIRED`를 던지는지 확인
- `resolveArgument`가 principal이 Long일 때 해당 값을 반환하는지 확인

mock/fake 전략:
- `SecurityContextHolder`, `Authentication`, `MethodParameter`를 mock으로 구성
- Spring context는 띄우지 않음
- 테스트 종료 후 SecurityContext clear
```

## 통합 테스트 계획

```text
대상:
- UserController의 `/me`, `/withdraw`
- LessonWaitingRoomService + LessonAdmissionScheduler
- LessonStockReconciliationScheduler
- 지역/위치 검색 회귀

필요 인프라:
- UserController: MockMvc 기반 slice 또는 controller integration, service는 mock
- Admission / Reconciliation / Search: Redis + PostgreSQL 필요

Testcontainers 적용 여부:
- Redis / PostgreSQL이 실제로 관여하는 admission, reconciliation, search 경로에는 적용한다
- 단위 테스트와 controller slice 테스트에는 적용하지 않는다

데이터 setup:
- 테스트별 고유 lessonId / requestId / userId 사용
- Redis key prefix와 dirty set, stream, status key를 테스트마다 분리
- PostgreSQL은 테스트 fixture insert 후 검증

데이터 cleanup:
- `@AfterEach`에서 Redis key 삭제
- repository delete 또는 transaction rollback으로 DB 정리
- 전역 flush는 피한다
```

## 각 테스트의 목적과 막는 위험

- `LoginUserArgumentResolverUnitTest`
  - 목적: 인증 해석 규칙을 고정한다.
  - 막는 위험: `@LoginUser`가 잘못된 principal을 통과시키는 회귀, 인증 실패가 누락되는 회귀

- `UserControllerIntegrationTest`
  - 목적: `@LoginUser`가 컨트롤러 파라미터로 주입되는지 확인한다.
  - 막는 위험: SecurityContext wiring 붕괴, 현재 사용자 조회/탈퇴가 잘못된 사용자 ID로 호출되는 회귀

- `LessonAdmissionSchedulerIntegrationTest`
  - 목적: 대기열 dequeue, stream enqueue, 실패 시 requeue를 고정한다.
  - 막는 위험: requestId 유실, FIFO 순서 붕괴, pipeline 실패 후 복구 실패, batch threshold 오작동

- `LessonStockReconciliationSchedulerIntegrationTest`
  - 목적: waiting room 잔여 메시지, stream lag, busy state에 따라 보정을 미루고, 조건이 정리되면 DB와 Redis를 맞춘다.
  - 막는 위험: 처리 중 상태 오판, 재고 오보정, dirty set 미정리, 음수 stock 회복 실패

- `LessonSearchRepositoryIntegrationTest`
  - 목적: 지역/위치/키워드 조합 검색이 기존 필터와 함께 동작하는지 고정한다.
  - 막는 위험: 검색 조건 누락, pagination/order 회귀

## 필요한 fixture / cleanup 전략

```text
- Resolver unit: SecurityContext mock, Authentication mock, MethodParameter fixture, 테스트 후 context clear
- Controller integration: MockMvc fixture, service mock, 별도 DB/Redis cleanup 불필요
- Redis integration: 레슨별 고유 key prefix, `waiting room key`, `status key`, `dirty set`, `stream key`, `busy key`, `last_active key`를 `@AfterEach`에서 삭제
- PostgreSQL integration: 테스트별 트랜잭션 롤백 우선, 비동기/스케줄러 경로는 repository delete 또는 명시적 teardown
- 공통 원칙: 전역 flush를 피하고, 테스트 전용 데이터만 정리한다
```

## 건드리면 안 되는 파일

```text
- src/main/java/**
- src/main/resources/**
- Redis key 정책 상수와 stream schema
- DB schema / migration
- 운영 profile 설정
- 기존 성능 테스트를 기능 테스트로 무리하게 전환하는 작업
- 이번 계획 단계에서의 build.gradle, src/test/resources/application-test.yml
```

## Human Approval 요청

```text
승인이 필요한 결정:
- Testcontainers 의존성 추가 여부
- build.gradle test task 변경 여부
- src/test/resources/application-test.yml을 container 주입형으로 바꿀지 여부
- Redis/PostgreSQL 통합 테스트용 공통 support class 추가 여부
- 새 통합 테스트 파일 생성 범위
- CI에서 container 기반 테스트를 실행할지 여부

수정 허용 파일:
- 승인 후에만 결정

수정 금지 파일:
- 운영 코드 전반
- Redis key 정책과 DB schema

추가 의존성:
- 승인 후에만 결정

검증 명령:
- ./gradlew test
- ./gradlew test --tests com.threestar.trainus.global.resolver.LoginUserArgumentResolverUnitTest
- ./gradlew test --tests com.threestar.trainus.domain.user.controller.UserControllerIntegrationTest
- ./gradlew test --tests com.threestar.trainus.domain.lesson.issue.LessonAdmissionSchedulerIntegrationTest
- ./gradlew test --tests com.threestar.trainus.domain.lesson.issue.LessonStockReconciliationSchedulerIntegrationTest
```

## 검증 명령

```text
계획 확정 전 확인용:
- ./gradlew test
- ./gradlew test --tests com.threestar.trainus.global.resolver.LoginUserArgumentResolverUnitTest
- ./gradlew test --tests com.threestar.trainus.domain.user.controller.UserControllerIntegrationTest
- ./gradlew test --tests com.threestar.trainus.domain.lesson.issue.LessonAdmissionSchedulerIntegrationTest
- ./gradlew test --tests com.threestar.trainus.domain.lesson.issue.LessonStockReconciliationSchedulerIntegrationTest

실제 구현 후:
- 위 targeted test 실행
- 전체 test 재실행
```

## 포트폴리오 기록 후보

```text
작업 후 주장 가능한 내용:
- 테스트 공백과 위험 경로를 근거 기반으로 분류했다.
- 단위 테스트와 통합 테스트 경계를 분리했다.
- Redis/PostgreSQL 의존 경로에만 Testcontainers 적용 여부를 판단했다.
- Human approval 필요 항목과 즉시 진행 가능 항목을 분리했다.

아직 주장하면 안 되는 내용:
- 테스트가 실제로 추가되었다는 주장
- 회귀가 실제로 막혔다는 주장
- coverage 수치 개선 주장
- Redis/PostgreSQL/Testcontainers가 실제로 동작 검증되었다는 주장
```
