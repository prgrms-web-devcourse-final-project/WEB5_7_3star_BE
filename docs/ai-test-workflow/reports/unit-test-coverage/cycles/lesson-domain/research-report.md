# Research Report

## 실행 정보

```text
실행 일시: 2026-06-13
사용 모델: gpt-5.4-mini
reasoning effort: high
에이전트 역할: Researcher
사용 프롬프트: lesson-domain unit-test coverage research
참조 문서:
- docs/ai-test-workflow/AGENTS.md
- docs/ai-test-workflow/rules/agent-roles.md
- docs/ai-test-workflow/rules/human-approval-policy.md
- docs/ai-test-workflow/rules/test-boundary.md
- docs/ai-test-workflow/rules/enforcement-checklist.md
- docs/ai-test-workflow/prompts/researcher.md
- docs/ai-test-workflow/templates/research-report-template.md
조사 범위:
- 레슨 강사/수강생 도메인의 main/test 코드
- 관련 service, mapper, dto, controller, scheduler, resolver, validation
- build.gradle 테스트 설정
- 외부 인프라 의존 여부
```

## 조사한 파일

```text
- build.gradle
- src/test/resources/application-test.yml
- src/main/resources/application.yml
- src/main/resources/application-local.yml
- src/main/resources/application-consumer.yml
- src/main/java/com/threestar/trainus/domain/lesson/teacher/controller/AdminLessonController.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/controller/LocationCsvController.java
- src/main/java/com/threestar/trainus/domain/lesson/student/controller/StudentLessonController.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/service/AdminLessonService.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/service/LessonCreationLimitService.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/service/LocationCsvService.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/scheduler/LessonStatusScheduler.java
- src/main/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonService.java
- src/main/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonFacade.java
- src/main/java/com/threestar/trainus/domain/lesson/student/mapper/LessonSearchMapper.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/mapper/LessonMapper.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/mapper/CreatedLessonMapper.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/mapper/LessonApplicationMapper.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/mapper/LessonParticipantMapper.java
- src/main/java/com/threestar/trainus/domain/lesson/student/mapper/LessonApplyMapper.java
- src/main/java/com/threestar/trainus/domain/lesson/student/mapper/LessonSimpleMapper.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/dto/LessonCreateRequestDto.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/dto/LessonUpdateRequestDto.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/dto/ApplicationActionRequestDto.java
- src/main/java/com/threestar/trainus/global/dto/PageRequestDto.java
- src/main/java/com/threestar/trainus/domain/lesson/student/dto/LessonSearchResponseDto.java
- src/main/java/com/threestar/trainus/domain/lesson/student/dto/LessonApplyRequestResponseDto.java
- src/main/java/com/threestar/trainus/domain/lesson/student/dto/LessonDetailResponseDto.java
- src/main/java/com/threestar/trainus/domain/lesson/issue/LessonApplyService.java
- src/main/java/com/threestar/trainus/domain/lesson/issue/LessonApplyConsumer.java
- src/main/java/com/threestar/trainus/domain/lesson/issue/LessonApplyProducer.java
- src/main/java/com/threestar/trainus/domain/lesson/issue/LessonWaitingRoomService.java
- src/main/java/com/threestar/trainus/domain/lesson/issue/LessonPendingMessageRecoveryScheduler.java
- src/main/java/com/threestar/trainus/domain/lesson/issue/LessonStockReconciliationScheduler.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/repository/LessonRepository.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/repository/LessonParticipantRepository.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/repository/LessonApplicationRepository.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/repository/LessonImageRepository.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/repository/LocationRepository.java
- src/test/java/com/threestar/trainus/domain/lesson/teacher/service/AdminLessonServiceTest.java
- src/test/java/com/threestar/trainus/domain/lesson/teacher/service/LessonCreationLimitServiceTest.java
- src/test/java/com/threestar/trainus/domain/lesson/teacher/scheduler/LessonStatusSchedulerTest.java
- src/test/java/com/threestar/trainus/domain/lesson/student/LessonApplyLockTest.java
- src/test/java/com/threestar/trainus/domain/lesson/student/LessonDatabaseSetupTest.java
- src/test/java/com/threestar/trainus/domain/lesson/student/LessonSearchPerformanceTest.java
- src/test/java/com/threestar/trainus/domain/lesson/student/KeywordSearchPerformanceTest.java (주석 처리됨)
- src/test/java/com/threestar/trainus/domain/lesson/student/LocationSearchPerformanceTest.java (주석 처리됨)
- src/test/java/com/threestar/trainus/global/resolver/LoginUserArgumentResolverUnitTest.java
```

## 현재 테스트 현황

| 영역 | 현재 테스트 | 비고 |
| --- | --- | --- |
| 단위 테스트 | `AdminLessonServiceTest`, `LessonCreationLimitServiceTest`, `LessonStatusSchedulerTest`, `LoginUserArgumentResolverUnitTest`(lesson 외 공통) | Mockito 기반. `AdminLessonServiceTest`는 생성/삭제/신청처리 일부만 커버. |
| 통합 테스트 | `LessonApplyLockTest`, `LessonSearchPerformanceTest`, `LessonDatabaseSetupTest` | `@SpringBootTest` + PostgreSQL/Redis 의존. 커버리지보다는 검증/벤치마크 성격이 강함. |
| Redis 관련 테스트 | `LessonCreationLimitServiceTest`, `LessonApplyLockTest` | `LessonApplyLockTest`는 실제 Redis Stream/락 경로를 사용. |
| PostgreSQL 관련 테스트 | `LessonSearchPerformanceTest`, `LessonDatabaseSetupTest`, `LessonApplyLockTest` | 테스트 프로파일도 PostgreSQL URL을 사용. |
| 스케줄러 테스트 | `LessonStatusSchedulerTest` | 상태 변경 호출 여부만 검증, 예외 처리 경로는 미검증. |
| 인증/인가 테스트 | `LoginUserArgumentResolverUnitTest`(공통) | lesson 전용 컨트롤러 테스트는 없음. |

보조 관찰:
- `KeywordSearchPerformanceTest`, `LocationSearchPerformanceTest`는 현재 주석 처리되어 있어 실행 테스트로는 보지 않음.
- lesson 도메인에는 컨트롤러 테스트, 매퍼 테스트, DTO 검증 테스트가 확인되지 않음.

## 테스트 공백

```text
1. StudentLessonController와 AdminLessonController에 대한 WebMvc/슬라이스 테스트가 없다. @LoginUser, @Valid, 요청 파라미터 검증, 응답 상태 코드가 실제로 고정되지 않았다.
2. AdminLessonService의 updateLesson, getLessonApplications, getLessonParticipants, getCreatedLessons, processLessonApplication의 DENIED/중복처리/권한/정원 초과 분기 등 주요 브랜치가 비어 있다.
3. StudentLessonService의 applyToApprovalLesson, applyToOpenRunLesson, cancelLessonApplication, getMyLessonApplications, getLessonDetail, getLessonSimple, getAsyncApplyStatus의 핵심 분기와 예외가 대부분 비어 있다.
4. Mapper 계층(LessonMapper, LessonSearchMapper, LessonApplyMapper, LessonSimpleMapper, CreatedLessonMapper, LessonApplicationMapper, LessonParticipantMapper)에 대한 순수 단위 테스트가 없다.
5. 요청 DTO 검증(LessonCreateRequestDto, LessonUpdateRequestDto, ApplicationActionRequestDto, PageRequestDto)과 helper 메서드(hasBasicInfoChanges, hasRestrictedChanges, hasTimeChanges)에 대한 테스트가 없다.
6. lesson issue 패키지의 Redis Stream consumer / pending recovery / stock reconciliation는 현재 실행되는 테스트가 없고, 통합 경로만 벤치마크 또는 수동 확인에 가깝다.
7. build.gradle에는 `test` task만 있고 `integrationTest` 분리가 없다. `h2` 의존성은 있으나 `application-test.yml`이 PostgreSQL/Redis를 직접 바라본다.
```

## 단위 테스트 후보

```text
- AdminLessonService
  - createLesson의 실패 분기: 종료시간 <= 시작시간, 최대인원 제한, 중복 레슨, 시간 충돌
  - updateLesson의 실패 분기: INVALID_REQUEST_DATA, LESSON_NOT_EDITABLE, LESSON_TIME_LIMIT_EXCEEDED, 참가자 존재 시 제한, 시간 충돌
  - processLessonApplication의 DENIED 경로와 이미 처리된 신청 예외
  - getLessonApplications / getLessonParticipants / getCreatedLessons의 ALL vs status 분기와 빈 목록 처리
- StudentLessonService
  - applyToApprovalLesson의 창립자 신청 금지, 중복 신청, 비모집 상태, openRun 차단
  - cancelLessonApplication의 openRun 차단, 신청 없음, APPROVED 취소 차단
  - getAsyncApplyStatus의 REQUEST_NOT_FOUND, WAITING, PROCESSING, SUCCESS 분기
  - getLessonDetail / getLessonSimple의 프로필 없음 예외와 단순 매핑
  - getMyLessonApplications의 status 파싱 실패와 ALL 분기
- Mapper
  - LessonMapper.toEntity / toResponseDto / toLessonDetailDto / toUpdateResponseDto
  - LessonSearchMapper.toLessonSearchResponseDto
  - LessonApplyMapper.toLessonApplicationResponseDto
  - LessonSimpleMapper.toLessonSimpleDto
  - CreatedLessonMapper와 LessonApplicationMapper / LessonParticipantMapper의 null/좌표 매핑
- Validation / DTO
  - LessonCreateRequestDto와 LessonUpdateRequestDto의 Bean Validation 제약
  - ApplicationActionRequestDto, PageRequestDto의 null/범위 검증
  - LessonUpdateRequestDto helper 메서드(hasBasicInfoChanges, hasRestrictedChanges, hasTimeChanges)
- LessonCsv
  - LocationCsvService의 checkLocation / CSV 처리 경계, 추정: MultipartFile 파싱과 LocationRepository 분기만 mock으로 검증 가능
```

## 통합 테스트로 넘길 후보

```text
- LessonApplyLockTest에 해당하는 open-run 동시성 경로(실제 Redis Stream, DB, 분산락)
- LessonApplyConsumer의 onMessage/scheduledProcess/processBuffer/processBatch/processChunks/processIndividually
- LessonPendingMessageRecoveryScheduler와 LessonStockReconciliationScheduler
- LessonSearchPerformanceTest가 다루는 LessonRepository 커스텀 검색 쿼리
- LessonDatabaseSetupTest의 대량 데이터 적재와 테이블 정리
- LessonApplyProducer의 Redis 재고 차감/Stream 발행 경로
- LessonWaitingRoomService의 enqueue/getRank/dequeue/requeueAfterAdmissionFailure
```

## 우선순위 높은 테스트 후보

| 우선순위 | 대상 | 필요한 이유 | 테스트 유형 |
| --- | --- | --- | --- |
| P1 | `AdminLessonService.updateLesson`와 `processLessonApplication` | 강사 핵심 수정/승인 흐름이며, 권한/시간/정원/상태 예외가 많다. 현재 일부만 커버됨. | 단위 |
| P1 | `StudentLessonService.applyToApprovalLesson`, `cancelLessonApplication`, `getAsyncApplyStatus` | 수강생 신청/취소/상태조회의 핵심 규칙이 있고, 레슨 상태와 중복/권한 예외가 회귀 위험이 높다. | 단위 |
| P2 | `LessonMapper`, `LessonSearchMapper`, `LessonSimpleMapper`, `LessonApplyMapper` | 응답 DTO의 좌표/이미지/상태 매핑이 깨지면 API 응답이 바로 흔들린다. | 단위 |
| P2 | 요청 DTO 검증 | controller 슬라이스 테스트가 없어서 `@Valid` 제약이 실제로 검증되지 않는다. | 단위 |
| P3 | `LessonApplyConsumer`와 재시도/회복 스케줄러 | Redis Stream 기반 비동기 처리의 실패 회귀를 잡는 핵심이지만, 외부 인프라 의존이 크다. | 통합 |

## Testcontainers 적용 검토

```text
필요 여부: 높음
적용 후보: LessonRepository 커스텀 조회, Redis Stream consumer/recovery, open-run 신청 흐름, location/keyword search 쿼리, lesson participant count 동기화
적용하지 않아도 되는 영역: 순수 서비스 분기 테스트, mapper, DTO validation
이유: `application-test.yml`이 PostgreSQL/Redis를 직접 참조하고, lesson domain의 핵심 경로가 DB/Redis 상태에 민감하다. H2 의존성은 있으나 현재 테스트 프로파일에서 실제로 사용되는 설정은 확인되지 않았다.
```

## Researcher 결론

```text
요약:
- lesson 도메인의 현재 실행 테스트는 일부 서비스 단위 테스트와 Redis/PostgreSQL 의존 통합형 검증, 성능/데이터셋 생성 테스트가 섞여 있다.
- 실제로 단위 테스트가 부족한 곳은 controller, mapper, DTO validation, 그리고 Admin/Student service의 주요 예외 분기다.
- 비동기/open-run/consumer/recovery/scheduler 흐름은 통합 테스트로 분리하는 편이 맞다.
- build.gradle은 test task만 있어 단위/통합 실행 경계가 아직 분리되어 있지 않다.

다음 단계에서 Planner가 판단해야 할 사항:
- P1으로 묶을 단위 테스트 범위와 우선순위
- controller 슬라이스 테스트를 이번 사이클에 포함할지 여부
- Redis/PostgreSQL 의존 경로를 Testcontainers로 넘길지, 로컬 고정 의존으로 둘지
- 현재 벤치마크성 테스트(LessonSearchPerformanceTest, LessonApplyLockTest)를 커버리지 산정에서 어떻게 취급할지
```

## Broad Coverage Follow-up Research - 2026-06-13

### 1. 이전 구현으로 해소된 공백

```text
- `AdminLessonService.updateLesson`과 `AdminLessonService.processLessonApplication`의 P1 분기 공백은 현재 사이클에서 해소되었다. 기존 계획 대비 수정 불가 상태, 시간 제한, 정원 초과 승인 예외, 이미 처리된 신청, 타 강사 접근, 거절 처리까지 단위 테스트로 고정되었다.
- `StudentLessonService.applyToApprovalLesson`, `StudentLessonService.cancelLessonApplication`, `StudentLessonService.getAsyncApplyStatus`의 핵심 분기도 현재 사이클에서 해소되었다. 창립자 신청 금지, 중복 신청, 비모집 상태, open-run 차단, 취소 차단, requestId 없음, WAITING/PROCESSING/SUCCESS 흐름이 포함된다.
- Reviewer가 지적한 `LESSON_MAX_PARTICIPANTS_EXCEEDED`와 직접 `PROCESSING` 상태 반환 분기도 이미 반영되었다.
```

### 2. 아직 남은 단위 테스트 공백

```text
- `AdminLessonService.createLesson`과 `AdminLessonService.deleteLesson`은 여전히 단위 테스트 공백이 크다. `validateLessonCreation`, `validateLessonDeletion`, `validateLessonAccess` 계열의 분기와 저장/이미지/재고 동기화 호출 여부는 별도 고정이 필요하다. 추정: 기존 사이클 산출물에 이 경로를 다룬 테스트는 없다.
- `AdminLessonService.getLessonApplications`, `getLessonParticipants`, `getCreatedLessons`는 페이지/상태 필터/빈 목록/카운트 래핑 분기가 남아 있다. 추정: 현재는 update/process 중심이라 조회 계열 응답 래핑은 비어 있다.
- `StudentLessonService.searchLessons`, `searchLessonsByLocation`, `getLessonDetail`, `getLessonSimple`, `getMyLessonApplications`는 아직 unit coverage가 없다. 특히 조회 계열은 `LessonSearchMapper`, `LessonSimpleMapper`, `LessonApplicationMapper`, `LessonMapper` 의존이 커서 응답 shape 회귀를 잡을 후보다.
- `StudentLessonService.applyToOpenRunLesson`의 producer 매핑은 아직 단위 테스트로 고정되지 않았다. `applyToOpenRunLesson -> lessonApplyProducer.send -> LessonWaitingRoomService.enqueue` 흐름은 unit에서 producer mocking으로 경계 검증은 가능하지만, Redis 상태 변화 자체는 통합 테스트가 더 적절하다.
- mapper/DTO/helper는 전반적으로 비어 있다. `LessonMapper`, `LessonSearchMapper`, `LessonApplyMapper`, `LessonSimpleMapper`, `CreatedLessonMapper`, `LessonApplicationMapper`, `LessonParticipantMapper`와 `LessonUpdateRequestDto.hasBasicInfoChanges/hasRestrictedChanges/hasTimeChanges`는 별도 unit test 후보다.
- controller slice는 단위 테스트와 분리해 별도 범주로 두는 것이 맞다. `AdminLessonController`와 `StudentLessonController`는 `@LoginUser`, `@Valid`, request param binding, status code 확인이 핵심이라 service unit test에 섞지 않는 편이 경계가 명확하다.
```

### 3. 넓은 커버리지 후보 우선순위 P1/P2/P3

| 우선순위 | 후보 | 근거 | 권장 경계 |
| --- | --- | --- | --- |
| P1 | `AdminLessonService.createLesson`, `AdminLessonService.deleteLesson` | 강사 핵심 생성/삭제는 회귀 영향이 크고, 생성 제한/시간 검증/권한/삭제 조건이 서비스 내부에 집중되어 있다. | 단위 |
| P1 | `StudentLessonService.applyToOpenRunLesson`, `getLessonDetail` | open-run 진입과 상세 조회는 사용자 체감 영향이 크고, producer 매핑과 상세 응답 shape 회귀를 막아야 한다. | 단위 |
| P2 | `AdminLessonService.getLessonApplications`, `getLessonParticipants`, `getCreatedLessons`, `StudentLessonService.searchLessons`, `searchLessonsByLocation`, `getLessonSimple`, `getMyLessonApplications` | 페이지/상태 필터/정렬/래핑/조회 매핑이 깨지면 API 응답이 바로 변한다. | 단위 |
| P2 | `LessonMapper`, `LessonSearchMapper`, `LessonApplyMapper`, `LessonSimpleMapper`, `CreatedLessonMapper`, `LessonApplicationMapper`, `LessonParticipantMapper` | DTO shape, 좌표, 이미지, 상태, count 래핑을 순수 함수 수준에서 잠그기 좋다. | 단위 |
| P2 | `LessonCreateRequestDto`, `LessonUpdateRequestDto`, `ApplicationActionRequestDto`, `PageRequestDto`, `LessonUpdateRequestDto` helper | `@Valid`와 helper 분기가 controller/service 입력 계약을 지키는지 확인해야 한다. | 단위 |
| P3 | `StudentLessonService.cancelPayment`, `completeParticipantPayment`, lock 계열 내부 분기 | 기능상 중요하지만 Redis/DB 상태 의존이 커서 단위보다 통합 쪽이 더 안정적이다. | 통합 우선 또는 제한적 단위 |

### 4. 통합 테스트로 넘길 후보

```text
- `LessonApplyProducer.send`와 `setStock`는 Redis 상태 변화, 중복 키, 재고 차감, 대기열 등록이 핵심이라 통합 후보다.
- `LessonApplyConsumer.onMessage`, `scheduledProcess`, `processBuffer`, `processBatch`, `processChunks`, `processIndividually`, `ackAndDelete`는 Redis Stream/ACK/DEL 동작 자체를 검증해야 하므로 통합 후보다.
- `LessonWaitingRoomService.enqueue`, `getRank`, `dequeue`, `requeueAfterAdmissionFailure`는 Sorted Set/TTL/랭킹 의미가 핵심이라 통합 후보다.
- `LessonPendingMessageRecoveryScheduler.recoverPendingMessages`와 `LessonStockReconciliationScheduler.reconcileStock`는 pending recovery, stream lag, stock reconciliation을 실제 Redis/PostgreSQL 상태와 함께 보아야 하므로 통합 후보다.
- `StudentLessonService.applyToLessonWithDistributedLock`, `applyToLessonWithPessimisticLock`, `applyToLessonWithCoupledLock`는 Redis/PostgreSQL/동시성 경로가 핵심이라 단위보다 통합이 맞다.
- `LessonRepository`의 검색 쿼리와 `searchLessonsByLocation`의 실제 공간/필터 결합은 PostgreSQL 연동 통합 테스트로 넘기는 편이 안전하다.
```

### 5. 다음 Planner가 판단해야 할 범위와 리스크

```text
- controller slice를 이번 사이클의 unit-test coverage 범주에 포함할지, 별도 WebMvc 범주로 분리할지 먼저 결정해야 한다.
- `StudentLessonService.applyToOpenRunLesson`을 unit에서 producer mocking으로만 고정할지, Redis Stream/Waiting Room까지 포함한 통합 시나리오로 넘길지 판단이 필요하다.
- mapper/DTO validation은 단위 테스트로 넓게 깔 수 있지만, 실제 회귀 영향은 서비스 조회/생성/수정 흐름보다 낮을 수 있다. coverage 숫자 확대만 목표로 쏠리지 않도록 우선순위를 조정해야 한다.
- Redis/PostgreSQL/consumer/recovery/open-run 동시성 경로는 단위 테스트로 억지로 묶기보다 통합 경계로 분리하는 편이 유지보수성이 좋다.
- 추정: 현재 남은 가장 큰 단위 테스트 공백은 생성/삭제/조회 서비스와 mapper/validation 계층이며, 비동기/동시성 경로는 단위보다 통합이 적합하다.
```

### 6. 조사한 파일 목록

```text
- src/main/java/com/threestar/trainus/domain/lesson/teacher/service/AdminLessonService.java
- src/main/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonService.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/controller/AdminLessonController.java
- src/main/java/com/threestar/trainus/domain/lesson/student/controller/StudentLessonController.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/dto/LessonCreateRequestDto.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/dto/LessonUpdateRequestDto.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/dto/ApplicationActionRequestDto.java
- src/main/java/com/threestar/trainus/global/dto/PageRequestDto.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/mapper/LessonMapper.java
- src/main/java/com/threestar/trainus/domain/lesson/student/mapper/LessonSearchMapper.java
- src/main/java/com/threestar/trainus/domain/lesson/student/mapper/LessonApplyMapper.java
- src/main/java/com/threestar/trainus/domain/lesson/student/mapper/LessonSimpleMapper.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/mapper/CreatedLessonMapper.java
- src/main/java/com/threestar/trainus/domain/lesson/student/mapper/LessonApplicationMapper.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/mapper/LessonParticipantMapper.java
- src/main/java/com/threestar/trainus/domain/lesson/issue/LessonApplyProducer.java
- src/main/java/com/threestar/trainus/domain/lesson/issue/LessonApplyConsumer.java
- src/main/java/com/threestar/trainus/domain/lesson/issue/LessonWaitingRoomService.java
- src/main/java/com/threestar/trainus/domain/lesson/issue/LessonPendingMessageRecoveryScheduler.java
- src/main/java/com/threestar/trainus/domain/lesson/issue/LessonStockReconciliationScheduler.java
- docs/ai-test-workflow/reports/unit-test-coverage/cycles/lesson-domain/test-plan.md
- docs/ai-test-workflow/reports/unit-test-coverage/cycles/lesson-domain/cycle-summary.md
- docs/ai-test-workflow/reports/unit-test-coverage/cycles/lesson-domain/review-report.md
```
