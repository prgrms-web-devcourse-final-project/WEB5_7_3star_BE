# 테스트 계획

## 실행 정보

```text
실행 시각: 2026-06-16 17:09:16 KST
사용 모델: gpt-5.4-mini
reasoning effort: high
에이전트 역할: Planner
사용 프롬프트: lesson Redis Stream failure cases planning
참조 문서:
- docs/ai-test-workflow/AGENTS.md
- docs/ai-test-workflow/rules/agent-roles.md
- docs/ai-test-workflow/rules/human-approval-policy.md
- docs/ai-test-workflow/rules/test-boundary.md
- docs/ai-test-workflow/rules/enforcement-checklist.md
- docs/ai-test-workflow/prompts/planner.md
- docs/ai-test-workflow/templates/test-plan-template.md
- docs/ai-test-workflow/reports/integration-test/cycles/project-wide-integration-gaps/research-report.md
- docs/ai-test-workflow/reports/integration-test/cycles/project-wide-integration-gaps/cycle-summary.md
- docs/ai-test-workflow/reports/unit-test-coverage/cycles/lesson-domain-unit-coverage/research-report.md
- docs/ai-test-workflow/reports/unit-test-coverage/cycles/lesson-domain-unit-coverage/cycle-summary.md
참조한 Researcher 결과:
- 레슨 Redis Stream의 formal integration coverage는 happy path 1건뿐이다.
- 후속 실패 후보는 중복 신청, 초과 신청, pending recovery, ACK/XDEL 실패 경로였다.
- 레슨 issue 패키지의 producer / admission / consumer / recovery / reconciliation 경로는 Redis와 PostgreSQL 상태에 민감하다.
```

## 작업 목표

```text
이번 작업의 목표:
- 레슨 도메인의 Redis Stream E2E에 대해 실패 안전성만 최소 범위로 추가한다.
- 기존 happy-path integration coverage 위에 producer / admission failure 계열을 얹어, 요청 유실과 Redis 상태 불일치를 막는 회귀를 고정한다.
- 이번 사이클에서는 consumer recovery, ACK/XDEL 세부 실패, stock reconciliation safety gate는 후속 분리 후보로 남긴다.

이번 작업에서 하지 않을 것:
- 운영 코드 수정
- build.gradle 수정
- src/test/resources/application-test.yml 수정
- controller slice / mapper / DTO validation 테스트 추가
- non-lesson domain / security / repository / scheduler(lesson issue 외) 범위 확장
- Testcontainers 하네스 재설계
- integrationTest Gradle task 신설
- 검증 실행
```

## 현재 happy-path 통합 테스트 커버리지

| 파일 | 현재 커버하는 흐름 | 아직 비어 있는 것 |
| --- | --- | --- |
| `src/test/java/com/threestar/trainus/domain/lesson/issue/LessonRedisStreamIntegrationTest.java` | `LessonApplyProducer.send` -> waiting room enqueue -> `LessonAdmissionScheduler.admitUsers` -> `LessonApplyConsumer` 성공 처리 -> `LessonStockReconciliationScheduler.reconcileStock` happy path | producer 거절, admission 재등록, consumer 실패/pending recovery, ACK/XDEL 실패, reconciliation safety gate |
| `src/test/java/com/threestar/trainus/domain/lesson/student/LessonApplyLockTest.java` | open-run 동시성 비교용 load test | formal integration contract가 아니며 Redis Stream 실패 분기를 고정하지 않음 |

보조 인프라:

- `src/test/java/com/threestar/trainus/testsupport/RedisStreamIntegrationTestSupport.java`
- `src/test/java/com/threestar/trainus/testsupport/PostgresIntegrationTestSupport.java`

이 support 클래스들은 실행 기반일 뿐 coverage 자체는 아니다.

## 대표 실패 후보

| 후보 | 필요한 이유 | 이번 사이클 처리 |
| --- | --- | --- |
| producer duplicate reject | 중복 신청이 실제 Redis set에만 걸리고 stock/waiting room/stream이 오염되지 않는지 확인해야 한다. | 이번 사이클 |
| producer stock exhaustion / pre-filter reject | 재고가 0 이하로 내려갈 때 requestId를 만들지 않고 duplicate key / stock key를 되돌리는지 확인해야 한다. | 이번 사이클 |
| admission requeue on missing status | waiting room에서 dequeue된 요청이 status write 또는 XADD 단계에서 유실되지 않아야 한다. | 이번 사이클 |
| admission requeue on malformed status | 잘못된 status payload가 들어와도 request가 waiting room으로 돌아가고 stream에 잘못 올라가지 않아야 한다. | 이번 사이클 |
| consumer batch/chunk fallback and pending retention | batch 실패 시 chunk/individual fallback이 작동하고, 시스템 실패 메시지가 pending으로 남는지 확인해야 한다. | 후속 분리 |
| pending recovery claim and replay | stale pending 메시지를 실제로 claim해서 재처리하는지 확인해야 한다. | 후속 분리 |
| reconciliation safety gate | waiting room 또는 stream lag가 남아 있으면 stock reconciliation이 앞서지 않아야 한다. | 후속 분리 |

## 이번 사이클의 최소 범위

```text
이번 사이클의 최소 구현 범위:
- 하나의 새 integration test class에서 producer rejection family와 admission requeue family만 고정한다.
- producer rejection family는 duplicate reject와 stock exhaustion / pre-filter reject 두 subcase를 포함한다.
- admission requeue family는 missing status와 malformed status 두 subcase를 포함한다.
- consumer recovery, ACK/XDEL failure, pending replay, reconciliation safety gate는 다음 사이클로 분리한다.
```

권장 파일 범위:

- `src/test/java/com/threestar/trainus/domain/lesson/issue/LessonRedisStreamFailureIntegrationTest.java`

조건부 허용:

- `src/test/java/com/threestar/trainus/testsupport/RedisStreamIntegrationTestSupport.java`
  - 새 failure test를 구현하는 데 공통 cleanup helper가 정말 필요할 때만 최소 수정

## 단위 테스트와 통합 테스트 경계

```text
단위 테스트 경계:
- AdminLessonServiceTest, StudentLessonServiceTest, producer/service branch tests는 이미 service-level rejection을 다룬다.
- 같은 예외를 Mockito 단위 테스트로 다시 복제하지 않는다.

통합 테스트 경계:
- 실제 Redis key mutation
- waiting room sorted set / stream / dirty set 상태
- producer -> admission handoff
- Redis state rollback / requeue / cleanup

이번 사이클의 통합 테스트가 검증할 것:
- Redis set / value / zset / stream의 실제 변경
- failure 시 request 유실 방지
- failure 시 잘못된 stream append 방지

이번 사이클의 통합 테스트가 검증하지 않을 것:
- controller binding
- mapper 변환
- DTO validation
- consumer pending recovery
- stock reconciliation safety gate
- repository query correctness
```

## 테스트 보강 우선순위

| 순서 | 대상 | 테스트 유형 | 목적 | 예상 변경 파일 |
| --- | --- | --- | --- | --- |
| 1 | producer duplicate reject / stock exhaustion | 통합 | Redis duplicate set과 stock rollback이 실제로 유지되는지 고정한다. | `src/test/java/com/threestar/trainus/domain/lesson/issue/LessonRedisStreamFailureIntegrationTest.java` |
| 2 | admission requeue on missing / malformed status | 통합 | waiting room dequeue 후 상태 전이 실패 시 request 유실을 막는다. | `src/test/java/com/threestar/trainus/domain/lesson/issue/LessonRedisStreamFailureIntegrationTest.java` |
| 3 | consumer recovery / reconciliation later split | 후속 | pending recovery, ACK/XDEL failure, lag gate는 별도 사이클로 분리한다. | 후속 cycle |

## 필요한 인프라 / fixture

```text
필요 인프라:
- Redis Testcontainers
- PostgreSQL/PostGIS Testcontainers
- Spring Boot test context
- `integration-test` profile

필요 fixture:
- teacher 1명
- student 1명
- open-run lesson 1건
- initial stock key 1건
- waiting room zset 1건
- admission용 status key 1건
- consumer group 1건 (admission failure test에서만 필요)

fixture 전략:
- 각 test는 고유한 email / nickname / lesson name을 사용한다.
- producer rejection test는 stream group 없이도 시작 가능하게 만든다.
- admission requeue test는 waiting room dequeue와 status payload 조작을 분리해서 준비한다.
- private helper는 test class 내부 factory method로 둔다.
```

## 데이터 setup / cleanup 전략

```text
setup:
- 각 test 시작 시 teacher / student / lesson / stock key를 최소 fixture로 생성한다.
- producer rejection family는 stock 0 또는 duplicate set이 명확한 상태를 직접 만든다.
- admission requeue family는 waiting room entry와 status key를 만들어 dequeue 이후 실패를 재현한다.

cleanup:
- `RedisStreamIntegrationTestSupport`의 Redis wildcard delete를 기본 cleanup으로 사용한다.
- DB는 `lesson_participants`, `lesson_application`, `lesson`, `user` 순으로 비우는 현재 패턴을 유지한다.
- stream key, duplicate key, stock key, status key, waiting room key, dirty set, busy key는 test 종료 후 남기지 않는다.
- pending entry가 생기면 stream key 삭제로 함께 제거한다.
```

## 허용 파일

```text
이번 사이클에서 승인 요청할 수정 허용 파일:
- src/test/java/com/threestar/trainus/domain/lesson/issue/LessonRedisStreamFailureIntegrationTest.java

조건부로만 허용:
- src/test/java/com/threestar/trainus/testsupport/RedisStreamIntegrationTestSupport.java
  - 새 failure test를 구현하는 데 공통 cleanup helper가 정말 필요할 때만 최소 수정
```

## 금지 파일

```text
- src/main/java/**
- src/main/resources/**
- build.gradle
- src/test/resources/application-test.yml
- src/test/java/com/threestar/trainus/domain/lesson/student/**
- src/test/java/com/threestar/trainus/domain/lesson/teacher/**
- src/test/java/com/threestar/trainus/domain/lesson/*/mapper/**
- src/test/java/com/threestar/trainus/domain/lesson/*/dto/**
- src/test/java/com/threestar/trainus/domain/lesson/*/repository/**
- src/test/java/com/threestar/trainus/domain/lesson/issue/LessonRedisStreamIntegrationTest.java
- src/test/java/com/threestar/trainus/domain/lesson/student/LessonApplyLockTest.java
- docs/** (이번 사이클의 test-plan.md / prompt-and-tool-log.md 제외)
- .github/**
```

## 검증 명령 후보

```text
- ./gradlew test --tests "com.threestar.trainus.domain.lesson.issue.LessonRedisStreamFailureIntegrationTest"
- ./gradlew test --tests "com.threestar.trainus.domain.lesson.issue.LessonRedisStreamIntegrationTest" --tests "com.threestar.trainus.domain.lesson.issue.LessonRedisStreamFailureIntegrationTest"
- ./gradlew test --tests "com.threestar.trainus.domain.lesson.issue.*IntegrationTest"
- ./gradlew --no-daemon test --tests '*IntegrationTest' --rerun-tasks
```

## 후속 분리

```text
이번 사이클 이후로 분리할 failure cases:
- LessonApplyConsumer의 batch/chunk fallback 및 individual failure 처리
- LessonPendingMessageRecoveryScheduler의 stale pending claim / replay
- LessonStockReconciliationScheduler의 waiting room / stream lag safety gate
- ACK/XDEL 실패가 stream cleanup에 남기는 residue 검증
- stream group bootstrap BUSYGROUP / listener wiring robustness
```

## Human Approval 요청

```text
승인이 필요한 결정:
- lesson Redis Stream failure coverage를 happy-path 1건 위에 최소 failure family 2개로만 확장하는 것
- producer rejection family와 admission requeue family를 이번 사이클에만 구현하고, recovery / ACK-XDEL / reconciliation gate는 후속으로 남기는 것
- 새 테스트는 단일 integration test class로 묶고, 기존 happy-path test나 운영 코드에는 손대지 않는 것

수정 허용 파일:
- src/test/java/com/threestar/trainus/domain/lesson/issue/LessonRedisStreamFailureIntegrationTest.java
- 조건부로만 `src/test/java/com/threestar/trainus/testsupport/RedisStreamIntegrationTestSupport.java`

수정 금지 파일:
- src/main/java/**
- build.gradle
- src/test/resources/application-test.yml
- 기존 lesson unit test / mapper test / DTO test / repository test
- `src/test/java/com/threestar/trainus/domain/lesson/issue/LessonRedisStreamIntegrationTest.java`
- `docs/**` (이번 사이클의 두 문서 제외)

추가 의존성:
- 없음
- Testcontainers 추가 없음
- integrationTest task 추가 없음

검증 명령:
- `./gradlew test --tests "com.threestar.trainus.domain.lesson.issue.LessonRedisStreamFailureIntegrationTest"`
- `./gradlew test --tests "com.threestar.trainus.domain.lesson.issue.LessonRedisStreamIntegrationTest" --tests "com.threestar.trainus.domain.lesson.issue.LessonRedisStreamFailureIntegrationTest"`

보류 사항:
- consumer recovery, ACK/XDEL failure, reconciliation lag gate는 이번 승인 범위에서 제외
- producer / admission failure test를 넘어서는 확장 범위는 다음 사이클에서 다시 승인받기
```
