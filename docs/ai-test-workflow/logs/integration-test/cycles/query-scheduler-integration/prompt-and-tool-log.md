# 프롬프트 및 도구 로그

## 조사 프롬프트

> 너는 Researcher다. 아래 규칙을 지켜라.
>
> - docs/ai-test-workflow/AGENTS.md를 먼저 읽고, agent-roles/human-approval-policy/test-boundary/enforcement-checklist도 확인하라.
> - 절대 codegraph를 사용하지 말고, rg와 sed로만 조사하라.
> - 파일 수정은 하지 말고, `docs/ai-test-workflow/reports/integration-test/cycles/query-scheduler-integration/` 아래에만 `research-report.md`와 `prompt-and-tool-log.md`를 작성하라.
> - 모든 산출물은 한국어를 기본으로 작성하라.
> - 조사 주제는 P2 통합 테스트 후보 조사이며 범위는 다음 3개다:
>   1) PostgreSQL/PostGIS 레슨 검색 쿼리
>   2) 레슨 재고 보정 스케줄러
>   3) 프로필 메타데이터 스케줄러
> - 반드시 현재 테스트 존재 여부, 통합 테스트가 필요한 이유, 단위 테스트와의 경계, Testcontainers 적합성, 데이터 setup/cleanup, 후속 분리 후보를 평가하라.
> - 현재 후보별로 다음을 조사하라:
>   - 검색 결과 correctness / 페이징 / count / 지역 조건 / 위치 기반 거리 검색
>   - DB count와 Redis stock 정합성 / 보정 실행 조건 / ShedLock 또는 중복 실행 방지 경계 / batch-consumer 처리 중 보정 타이밍
>   - 리뷰 count 집계 / 평점 집계 / 프로필 메타데이터 반영 / DB side effect 검증
> - 우선 읽을 파일 후보:
>   - src/main/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonService.java
>   - src/main/java/com/threestar/trainus/domain/lesson/teacher/repository/LessonRepository.java
>   - src/main/java/com/threestar/trainus/domain/lesson/issue/LessonStockReconciliationScheduler.java
>   - src/main/java/com/threestar/trainus/domain/metadata/service/ProfileMetadataService.java
>   - src/main/java/com/threestar/trainus/domain/metadata/scheduler/ProfileMetadataScheduler.java
>   - src/main/java/com/threestar/trainus/domain/review/repository/ReviewRepository.java
>   - src/test/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonServiceTest.java
>   - src/test/java/com/threestar/trainus/domain/lesson/student/LessonSearchPerformanceTest.java
>   - src/test/java/com/threestar/trainus/domain/lesson/student/LocationSearchPerformanceTest.java
>   - src/test/java/com/threestar/trainus/domain/metadata/service/ProfileMetadataServiceTest.java
> - 성능 테스트는 통합 회귀 테스트 후보와 분리해서 평가하라. 성능 수치나 벤치마크 결과를 regression coverage로 주장하지 말라.
> - 결과는 구체적인 파일 경로와 근거를 함께 남겨라.
> - 조사 결과에서 각 후보의 현재 테스트 존재 여부와 아직 비어 있는 통합 테스트 공백을 명확히 적어라.
> - 완료 후에는 종료하고, 구현 계획은 작성하지 말라.
> - 리포트에는 프롬프트/도구/모델/reasoning effort/토큰 사용량 기록도 포함하라.

## 사용 도구와 확인 내용

### 1. 문서 규칙 확인

- `sed -n '1,260p' docs/ai-test-workflow/AGENTS.md`
- `sed -n '1,260p' docs/ai-test-workflow/rules/agent-roles.md`
- `sed -n '1,260p' docs/ai-test-workflow/rules/human-approval-policy.md`
- `sed -n '1,260p' docs/ai-test-workflow/rules/test-boundary.md`
- `sed -n '1,260p' docs/ai-test-workflow/rules/enforcement-checklist.md`

확인한 내용:
- Researcher는 조사만 수행하고 코드 수정 금지
- 연구 산출물은 `research-report.md`와 `prompt-and-tool-log.md`만 작성
- 통합 테스트는 Spring context, DB, Redis, scheduler 등 실제 연결 경로 검증에 적합
- Testcontainers는 PostgreSQL/Redis가 필요한 경우 우선 검토

### 2. 파일/테스트 현황 탐색

- `rg --files src/main/java src/test/java`
- `rg -n "class StudentLessonService|class LessonRepository|class LessonStockReconciliationScheduler|class ProfileMetadataService|class ProfileMetadataScheduler|interface ReviewRepository|class StudentLessonServiceTest|class LessonSearchPerformanceTest|class LocationSearchPerformanceTest|class ProfileMetadataServiceTest"`
- `sed -n '1,320p' build.gradle`
- `sed -n '1,240p' src/test/resources/application-test.yml`
- `sed -n '1,240p' src/main/resources/application.yml`

확인한 내용:
- `testImplementation`에 H2와 Testcontainers PostgreSQL/JUnit이 모두 존재
- `tasks.named('test')`에서 `DOCKER_HOST`와 `DOCKER_API_VERSION`을 설정
- 테스트 프로필은 PostgreSQL/PostGIS와 Redis를 염두에 둔 구성이다

### 3. 대상 코드 조사

- `sed -n '1,320p' src/main/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonService.java`
- `sed -n '1,360p' src/main/java/com/threestar/trainus/domain/lesson/teacher/repository/LessonRepository.java`
- `sed -n '400,520p' src/main/java/com/threestar/trainus/domain/lesson/teacher/repository/LessonRepository.java`
- `sed -n '1,320p' src/main/java/com/threestar/trainus/domain/lesson/issue/LessonStockReconciliationScheduler.java`
- `sed -n '1,260p' src/main/java/com/threestar/trainus/domain/metadata/service/ProfileMetadataService.java`
- `sed -n '1,240p' src/main/java/com/threestar/trainus/domain/metadata/scheduler/ProfileMetadataScheduler.java`
- `sed -n '1,220p' src/main/java/com/threestar/trainus/domain/review/repository/ReviewRepository.java`
- `sed -n '1,220p' src/main/java/com/threestar/trainus/domain/lesson/issue/LessonApplyConsumer.java`
- `sed -n '1,220p' src/main/java/com/threestar/trainus/domain/lesson/teacher/entity/Lesson.java`
- `sed -n '1,200p' src/main/java/com/threestar/trainus/global/utils/PageLimitCalculator.java`

확인한 내용:
- 검색 쿼리는 native query와 PostGIS 거리 조건을 직접 사용
- 재고 보정 스케줄러는 Redis busy/lastActive/dirty set/stream backlog를 함께 본다
- 프로필 메타데이터는 리뷰 count와 average rating을 DB에서 다시 계산해 반영한다

### 4. 테스트 코드 조사

- `sed -n '1,320p' src/test/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonServiceTest.java`
- `sed -n '320,640p' src/test/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonServiceTest.java`
- `sed -n '1,320p' src/test/java/com/threestar/trainus/domain/lesson/student/LessonSearchPerformanceTest.java`
- `sed -n '1,260p' src/test/java/com/threestar/trainus/domain/lesson/student/LocationSearchPerformanceTest.java`
- `sed -n '1,260p' src/test/java/com/threestar/trainus/domain/lesson/student/KeywordSearchPerformanceTest.java`
- `sed -n '1,320p' src/test/java/com/threestar/trainus/domain/metadata/service/ProfileMetadataServiceTest.java`
- `sed -n '1,260p' src/test/java/com/threestar/trainus/domain/lesson/issue/LessonRedisStreamIntegrationTest.java`
- `sed -n '1,260p' src/test/java/com/threestar/trainus/domain/lesson/issue/LessonRedisStreamFailureIntegrationTest.java`
- `sed -n '1,240p' src/test/java/com/threestar/trainus/domain/review/controller/ReviewControllerTest.java`
- `sed -n '1,260p' src/test/java/com/threestar/trainus/domain/lesson/teacher/scheduler/LessonStatusSchedulerTest.java`

확인한 내용:
- 검색/위치 검색은 mock 기반 서비스 단위 테스트만 존재
- Redis Stream happy path와 failure path는 있으나 재고 보정 스케줄러 전용 테스트는 없다
- 프로필 메타데이터는 서비스 단위 테스트와 리뷰 생성 후 DB side effect 확인 테스트만 있다

### 5. 상태/목표 확인

- `functions.get_goal` 호출 결과: `goal=null`, `remainingTokens=null`, `completionBudgetReport=null`

해석:
- 세션에 별도 목표 객체가 없어서 토큰 잔량/예산 요약은 도구에서 수치로 제공되지 않았다

## 기록 메타

- 모델: GPT-5 Codex 세션
- reasoning effort: 높음
- 토큰 사용량: 수치 미제공, `get_goal` 기준 확인 불가
- 사용 도구: `rg`, `sed`, `functions.get_goal`, `mkdir -p`, `apply_patch`

## 추가 검증 기록

- `./gradlew --no-daemon test --tests "com.threestar.trainus.domain.lesson.issue.LessonRedisStreamFailureIntegrationTest"`
- `./gradlew --no-daemon test --tests "com.threestar.trainus.domain.lesson.teacher.repository.LessonRepositoryIntegrationTest"`
- `./gradlew --no-daemon test --tests "com.threestar.trainus.domain.lesson.issue.LessonStockReconciliationSchedulerIntegrationTest"`
- `./gradlew --no-daemon test --tests "com.threestar.trainus.global.config.security.JwtAuthenticationFilterIntegrationTest"`
- `./gradlew --no-daemon test --tests "com.threestar.trainus.domain.lesson.issue.LessonRedisStreamIntegrationTest"`

확인한 내용:

- 위 테스트들은 개별 실행 기준으로 모두 성공했다.
- 여러 `SpringBootTest`를 병렬로 묶으면 Testcontainers와 `build/test-results` 기록이 서로 간섭해서 실패했다.
- 따라서 이전 테스트 영향 여부는 병렬 결과가 아니라 개별 실행 결과로 판단해야 한다.

## Planner 실행 기록

```text
실행 일시: 2026-06-16 KST
역할: Planner

프롬프트 요약:
- Researcher의 `research-report.md`를 기준으로 P2 후보 3개에 대한 승인 가능한 최소 통합 테스트 계획을 작성했다.
- 조사 대상은 PostgreSQL/PostGIS 레슨 검색 쿼리, 레슨 재고 보정 스케줄러, 프로필 메타데이터 스케줄러였다.
- 반드시 포함해야 하는 항목인 현재 테스트 존재 여부, 통합 테스트 필요 이유, 단위 테스트 경계, 최소 승인 구현 범위, Testcontainers 여부, setup/cleanup, 허용/금지 파일, 검증 명령, 후속 분리 후보를 계획서에 반영했다.
- 승인 전 구현 금지, 운영 코드 수정 금지, 성능 테스트를 회귀 테스트로 섞지 않기, 승인 범위 밖 파일 수정 금지 원칙을 문서에 명시했다.

사용 도구:
- `rg --files`
- `sed -n`
- `exec_command`
- `multi_tool_use.parallel`
- `apply_patch`

사용 모델: gpt-5.4-mini
reasoning effort: high
토큰 사용량: 도구에서 제공되지 않음

연구 결과 반영 메모:
- 검색 쿼리는 `StudentLessonServiceTest`의 mock 분기 테스트만 존재하고 `LessonSearchPerformanceTest` / `LocationSearchPerformanceTest`는 정합성 회귀가 아니므로 repository 통합 테스트로 분리했다.
- 재고 보정 스케줄러는 `LessonRedisStreamIntegrationTest`의 happy path만 있으므로 busy / stream lag / dirty set / 음수 재고 / 파싱 실패를 잡는 별도 통합 테스트를 최소 범위로 잡았다.
- 프로필 메타데이터 스케줄러는 scheduler 자체 테스트가 없고 `ProfileMetadataServiceTest`만 있으므로 scheduler 통합 테스트를 신규 추가하는 범위로 정리했다.
- 성능 테스트는 회귀 테스트로 편입하지 않는다는 연구 결론을 그대로 유지했다.

작성 메모:
- 이번 단계에서는 계획만 작성했고 구현/검증은 수행하지 않았다.
- 운영 코드, 성능 테스트, build 설정, 공통 테스트 인프라 파일은 승인 범위 밖으로 두었다.
- codegraph는 사용하지 않았고, `rg`와 `sed` 중심으로 지침과 연구 보고서를 재확인했다.
```

## Reviewer 실행 기록

```text
실행 일시: 2026-06-16 KST
역할: Reviewer

사용 모델: gpt-5.5
reasoning effort: medium

검토 범위:
- 구현된 레슨 통합 테스트
- 공통 테스트 하네스와 설정 변경 영향
- 이전 테스트 영향 여부

결과 요약:
- 레슨 도메인 통합 테스트는 개별 실행 기준으로 통과했다.
- 공통 테스트 하네스와 설정 변경은 다른 테스트 실행 환경에 영향을 줄 수 있다.
- 메타데이터 스케줄러 테스트는 레슨-only 범위를 벗어난 것으로 지적했다.
```
