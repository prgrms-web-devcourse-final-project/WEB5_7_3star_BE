# Test Plan

## 실행 정보

```text
실행 일시: 2026-06-13
사용 모델: gpt-5.4-mini
reasoning effort: high
에이전트 역할: Planner
사용 프롬프트: lesson-domain-unit-coverage unit-test coverage planning
참조 문서:
- docs/ai-test-workflow/AGENTS.md
- docs/ai-test-workflow/rules/agent-roles.md
- docs/ai-test-workflow/rules/human-approval-policy.md
- docs/ai-test-workflow/rules/test-boundary.md
- docs/ai-test-workflow/rules/enforcement-checklist.md
- docs/ai-test-workflow/prompts/planner.md
- docs/ai-test-workflow/templates/test-plan-template.md
참조한 Research Report:
- docs/ai-test-workflow/reports/unit-test-coverage/cycles/lesson-domain-unit-coverage/research-report.md
```

## 작업 목표

```text
이번 작업의 목표:
- lesson-domain-unit-coverage의 강사/수강생 서비스에서 P1 단위 테스트 공백을 먼저 메운다.
- 기존 Mockito 기반 단위 테스트 패턴을 유지하면서 AdminLessonService와 StudentLessonService의 핵심 분기만 보강한다.
- 이번 사이클에서는 통합 테스트, 운영 코드 변경, 설정 변경, Testcontainers 추가를 하지 않는다.

이번 작업에서 하지 않을 것:
- controller slice/WebMvc 테스트 추가
- mapper/DTO validation 테스트 추가
- Redis Stream consumer/recovery, open-run 동시성, repository search, scheduler를 이번 사이클에서 통합 테스트로 구현
- build.gradle, test profile, application 설정, production code 수정
- 공통 테스트 support class/fixture 파일을 새로 만드는 확장
```

## 테스트 보강 우선순위

| 순서 | 대상 | 테스트 유형 | 목적 | 예상 변경 파일 |
| --- | --- | --- | --- | --- |
| 1 | `AdminLessonService.updateLesson`, `AdminLessonService.processLessonApplication` | 단위 | 수정 가능 여부, 정원/상태 분기, 승인/거절/중복 처리의 핵심 회귀를 먼저 고정한다. | `src/test/java/com/threestar/trainus/domain/lesson/teacher/service/AdminLessonServiceTest.java` |
| 2 | `StudentLessonService.applyToApprovalLesson`, `StudentLessonService.cancelLessonApplication`, `StudentLessonService.getAsyncApplyStatus` | 단위 | 수강생 신청/취소/비동기 상태 조회의 권한, 중복, 상태 전이 분기를 먼저 고정한다. | `src/test/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonServiceTest.java` |
| 3 | `LessonMapper`/`LessonApplicationMapper`/`LessonSimpleMapper` 및 DTO validation | 후속(P2) | 응답 매핑과 입력 검증을 고정하지만, 이번 승인 범위에서는 제외한다. | 후속 사이클에서 별도 계획 |

## 단위 테스트 계획

```text
대상:
- AdminLessonService의 updateLesson, processLessonApplication
- StudentLessonService의 applyToApprovalLesson, cancelLessonApplication, getAsyncApplyStatus

이번 사이클의 단위 테스트 범위:
- updateLesson의 실패 분기: 레슨 수정 불가 상태, 시간 제한 위반, 승인 참가자 존재 시 변경 제한, 잘못된 요청 데이터
- processLessonApplication의 거절 처리, 이미 처리된 신청 예외, 비정상 요청 재현
- applyToApprovalLesson의 창립자 신청 금지, 중복 신청, 비모집 상태, open-run 차단
- cancelLessonApplication의 open-run 취소 금지, 신청 없음, PENDING 이외 상태 취소 금지
- getAsyncApplyStatus의 requestId 없음, WAITING, PROCESSING, SUCCESS 분기

검증할 동작:
- 서비스 메서드가 분기별로 올바른 ErrorCode 또는 응답 상태를 반환하는지 확인한다.
- repository save/delete 호출 여부와 저장 객체의 상태 전이를 확인한다.
- Redis 조회 결과와 waiting room 조회 결과에 따라 비동기 상태가 올바르게 해석되는지 확인한다.

mock/fake 전략:
- `@ExtendWith(MockitoExtension.class)`와 `@InjectMocks`를 사용해 Spring context 없이 실행한다.
- `LessonRepository`, `LessonImageRepository`, `LessonApplicationRepository`, `LessonParticipantRepository`, `UserService`, `LessonCreationLimitService`, `LessonApplyProducer`, `ProfileRepository`, `ProfileMetadataService`, `StringRedisTemplate`, `LessonWaitingRoomService`는 Mockito mock으로 대체한다.
- `Lesson`, `User`, `Profile`, `LessonApplication`은 builder 기반 최소 fixture로 만든다.
- `mqRedisTemplate.opsForValue().get(...)`와 `waitingRoomService.getRank(...)`는 필요한 분기만 stub한다.
- 저장 객체 검증은 `ArgumentCaptor`로 필요한 경우에만 확인하고, fixture는 테스트 메서드 내부에 국소적으로 둔다.
```

## 통합 테스트 계획

```text
대상:
- LessonApplyProducer의 Redis Stream 발행 및 재고 차감 경로
- LessonApplyConsumer의 onMessage / batch / retry / recovery 흐름
- LessonPendingMessageRecoveryScheduler, LessonStockReconciliationScheduler
- LessonWaitingRoomService의 enqueue / dequeue / rank / requeueAfterAdmissionFailure
- lesson repository 검색 쿼리 및 open-run 동시성 경로

필요 인프라:
- Redis
- PostgreSQL

Testcontainers 적용 여부:
- 이번 사이클에서는 적용하지 않는다.
- 후속 통합 테스트 사이클에서 Redis/PostgreSQL가 필요한 항목에 한해 우선 검토한다.

데이터 setup:
- 이번 사이클에서는 작성하지 않는다.
- 후속 통합 사이클에서는 명시적 seed fixture와 테스트별 초기 상태 정의가 필요하다.

데이터 cleanup:
- 이번 사이클에서는 작성하지 않는다.
- 후속 통합 사이클에서는 테스트 종료 후 repository truncate 또는 transaction rollback 전략을 별도로 정의한다.
```

## Human Approval 요청

```text
승인이 필요한 결정:
- unit-test-coverage / lesson-domain-unit-coverage 사이클에서 AdminLessonServiceTest를 보강하고 StudentLessonServiceTest를 새로 추가하는 것
- 외부 인프라 없이 Mockito 기반 단위 테스트로만 P1 분기를 먼저 고정하는 것
- controller slice, mapper, DTO validation, consumer/recovery, scheduler, search 쿼리는 이번 승인 범위에서 제외하는 것

수정 허용 파일:
- src/test/java/com/threestar/trainus/domain/lesson/teacher/service/AdminLessonServiceTest.java
- src/test/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonServiceTest.java

수정 금지 파일:
- src/main/java/** 전체
- build.gradle
- src/test/resources/application-test.yml
- src/test/java/com/threestar/trainus/domain/lesson/issue/**
- src/test/java/com/threestar/trainus/domain/lesson/*/controller/**
- src/test/java/com/threestar/trainus/domain/lesson/*/mapper/**
- src/test/java/com/threestar/trainus/domain/lesson/*/repository/**
- docs/ai-test-workflow/reports/unit-test-coverage/cycles/lesson-domain-unit-coverage/research-report.md
- docs/ai-test-workflow/reports/unit-test-coverage/cycles/lesson-domain-unit-coverage/cycle-summary.md

추가 의존성:
- 없음
- Testcontainers 추가 없음
- build.gradle 변경 없음

검증 명령:
- `./gradlew test --tests "com.threestar.trainus.domain.lesson.teacher.service.AdminLessonServiceTest" --tests "com.threestar.trainus.domain.lesson.student.service.StudentLessonServiceTest"`
- 필요 시 `./gradlew test`
```

## Broad Coverage Follow-up Test Plan - 2026-06-13

### 실행 정보

- 실행 일시: 2026-06-13
- 사용 모델: gpt-5.4-mini
- reasoning effort: high
- 토큰 사용량: 서브에이전트에 노출되지 않음

### 작업 목표

- 기존 lesson-domain-unit-coverage 사이클의 단위 테스트 경계를 유지하면서, 남은 서비스 조회/생성/삭제 분기와 mapper/DTO/helper까지 포함해 레슨 도메인 커버리지를 한 단계 더 넓힌다.
- controller slice, Redis/PostgreSQL 통합 경로, consumer/recovery/open-run 동시성은 이번 승인 범위에서 제외하고 후속 통합 후보로만 유지한다.

### 이번 작업에서 하지 않을 것

- controller slice/WebMvc 테스트 구현
- 통합 테스트, Testcontainers, `build.gradle`, test profile, 운영 코드 수정
- Redis Stream consumer/recovery, scheduler, repository search, open-run 동시성의 실제 통합 검증
- 기존 `cycle-summary`, `research-report`, `review-report`, `pr-draft` 수정

### 테스트 보강 우선순위

| 순서 | 대상 | 테스트 유형 | 목적 | 예상 변경 파일 |
| --- | --- | --- | --- | --- |
| 1 | `AdminLessonService.createLesson`, `AdminLessonService.deleteLesson` | 단위 | 생성 제한, 권한, 삭제 조건, 이미지/재고 동기화 호출 여부를 먼저 고정한다. | `src/test/java/com/threestar/trainus/domain/lesson/teacher/service/AdminLessonServiceTest.java` |
| 2 | `StudentLessonService.applyToOpenRunLesson`, `StudentLessonService.getLessonDetail` | 단위 | open-run 진입의 producer 경계와 상세 조회 응답 shape 회귀를 먼저 고정한다. | `src/test/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonServiceTest.java` |
| 3 | `AdminLessonService.getLessonApplications`, `getLessonParticipants`, `getCreatedLessons`, `StudentLessonService.searchLessons`, `searchLessonsByLocation`, `getLessonSimple`, `getMyLessonApplications` | 단위 | 페이지/상태 필터/래핑/조회 분기의 회귀를 넓게 잠근다. | `src/test/java/com/threestar/trainus/domain/lesson/teacher/service/AdminLessonServiceTest.java`, `src/test/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonServiceTest.java` |
| 4 | `LessonMapper`, `LessonSearchMapper`, `LessonApplyMapper`, `LessonSimpleMapper`, `CreatedLessonMapper`, `LessonApplicationMapper`, `LessonParticipantMapper` | 단위 | DTO shape, 좌표, 이미지, 상태, count 래핑을 순수 함수 수준에서 검증한다. | 신규 mapper 테스트 파일 또는 기존 service test 보강 |
| 5 | `LessonCreateRequestDto`, `LessonUpdateRequestDto`, `ApplicationActionRequestDto`, `PageRequestDto`, `LessonUpdateRequestDto` helper | 단위 | 입력 계약과 helper 분기를 `@Valid` 수준에서 고정한다. | 신규 DTO/helper 테스트 파일 |

### 단위 테스트 계획

- 대상:
  - `AdminLessonService`의 `createLesson`, `deleteLesson`, `getLessonApplications`, `getLessonParticipants`, `getCreatedLessons`
  - `StudentLessonService`의 `applyToOpenRunLesson`, `getLessonDetail`, `searchLessons`, `searchLessonsByLocation`, `getLessonSimple`, `getMyLessonApplications`
  - mapper/DTO/helper 대상: `LessonMapper`, `LessonSearchMapper`, `LessonApplyMapper`, `LessonSimpleMapper`, `CreatedLessonMapper`, `LessonApplicationMapper`, `LessonParticipantMapper`, `LessonCreateRequestDto`, `LessonUpdateRequestDto`, `ApplicationActionRequestDto`, `PageRequestDto`
- 검증할 동작:
  - create/delete 계열의 권한, 시간, 정원, 중복, 삭제 가능 상태, 저장/삭제/동기화 호출 분기를 확인한다.
  - 조회 계열의 page/status filter, 빈 목록, count 래핑, 응답 DTO shape을 확인한다.
  - open-run 진입은 producer 호출 여부까지만 단위로 검증하고, Redis/대기열 상태 변화는 통합 후보로 남긴다.
  - DTO/helper는 Bean Validation 제약과 helper boolean 분기를 고정한다.
- mock/fake 전략:
  - Spring context 없이 `@ExtendWith(MockitoExtension.class)` 중심으로 실행한다.
  - repository, producer, redis template, waiting room service, profile service, user service는 Mockito mock으로 대체한다.
  - mapper와 DTO/helper는 가능하면 순수 객체 fixture만 사용하고, 외부 상태가 필요한 경우 최소 stub만 둔다.
  - 공통 fixture는 새 support class보다 테스트 내부 factory 메서드로 우선 구성한다.

### 통합 테스트 경계

- 대상:
  - `LessonApplyConsumer`의 `onMessage` / `scheduledProcess` / `processBuffer` / `processBatch` / `processChunks` / `processIndividually`
  - `LessonPendingMessageRecoveryScheduler`, `LessonStockReconciliationScheduler`
  - `LessonWaitingRoomService`의 `enqueue` / `dequeue` / `getRank` / `requeueAfterAdmissionFailure`
  - lesson repository 검색 쿼리와 open-run 실제 동시성 경로
- 경계 원칙:
  - Redis/PostgreSQL/consumer/recovery/open-run 실제 동시성은 통합 후보로만 둔다.
  - Testcontainers, `integrationTest` task 분리, `build.gradle` 변경은 이번 승인 범위에서 제외한다.
  - controller slice는 이번 broad follow-up에서도 제외하고 별도 범주로 둔다.

### 우선 구현 순서

1. `AdminLessonService`의 create/delete 및 조회 분기부터 넓혀 서비스 핵심 응답을 먼저 잠근다.
2. `StudentLessonService`의 open-run 진입, 상세 조회, 검색/목록 조회 분기를 이어서 고정한다.
3. mapper와 DTO/helper 단위를 별도 테스트 파일로 분리해 응답 shape과 입력 계약을 잠근다.
4. 이후 남는 회귀는 통합 후보로만 분류하고, 실제 Redis/PostgreSQL 경로는 다음 승인 사이클로 넘긴다.

### 각 테스트 묶음의 목적

- 생성/삭제 서비스 묶음: 레슨 운영자 핵심 변화를 가장 먼저 잡는다.
- 조회 서비스 묶음: 응답 shape, 페이징, 필터, 빈 목록의 회귀를 줄인다.
- open-run 묶음: producer 경계와 비동기 진입점을 단위 수준에서 잠근다.
- mapper/DTO/helper 묶음: 서비스 로직보다 넓은 회귀를 낮은 비용으로 고정한다.

### 수정 허용 파일

- `src/test/java/com/threestar/trainus/domain/lesson/teacher/service/AdminLessonServiceTest.java`
- `src/test/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonServiceTest.java`
- 필요 시 `src/test/java/com/threestar/trainus/domain/lesson/teacher/mapper/*`
- 필요 시 `src/test/java/com/threestar/trainus/domain/lesson/student/mapper/*`
- 필요 시 `src/test/java/com/threestar/trainus/domain/lesson/teacher/dto/*`
- 필요 시 `src/test/java/com/threestar/trainus/domain/lesson/student/dto/*`
- 필요 시 `src/test/java/com/threestar/trainus/global/dto/*`

### 수정 금지 파일

- `src/main/java/**`
- `build.gradle`
- `src/test/resources/application-test.yml`
- `src/test/java/com/threestar/trainus/domain/lesson/issue/**`
- `src/test/java/com/threestar/trainus/domain/lesson/*/controller/**`
- `docs/ai-test-workflow/reports/unit-test-coverage/cycles/lesson-domain-unit-coverage/research-report.md`
- `docs/ai-test-workflow/reports/unit-test-coverage/cycles/lesson-domain-unit-coverage/cycle-summary.md`
- `docs/ai-test-workflow/reports/unit-test-coverage/cycles/lesson-domain-unit-coverage/review-report.md`

### 사용자 승인 필요 사항

- `AdminLessonServiceTest`와 `StudentLessonServiceTest`를 넘어 mapper/DTO/helper 단위 테스트까지 확장하는 것
- controller slice와 통합 테스트는 이번 승인 범위에서 제외하는 것
- Redis/PostgreSQL/consumer/recovery/open-run 경로는 실제 통합 구현 대신 후보로만 남기는 것
- Testcontainers, `build.gradle`, test profile 변경 없이 Mockito 기반 단위 테스트만 넓히는 것

### 검증 명령

- `./gradlew test --tests "com.threestar.trainus.domain.lesson.teacher.service.AdminLessonServiceTest" --tests "com.threestar.trainus.domain.lesson.student.service.StudentLessonServiceTest"`
- mapper/DTO/helper 파일이 추가되면 해당 패키지만 골라 실행하는 `./gradlew test --tests "com.threestar.trainus.domain.lesson.*.*"`
- 필요 시 `./gradlew test`
