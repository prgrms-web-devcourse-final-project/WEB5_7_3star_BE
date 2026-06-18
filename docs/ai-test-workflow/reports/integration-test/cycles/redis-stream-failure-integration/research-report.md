# 레슨 Redis Stream 실패 케이스 연구 보고서

## 실행 정보

| 항목 | 값 |
| --- | --- |
| 실행 시각 | 2026-06-16T08:10:28Z |
| 모델 | gpt-5.4-mini |
| 역할 | Researcher |
| 작업 흐름 | integration-test |
| 사이클 | redis-stream-failure-integration |
| 범위 | 레슨 도메인만 |
| Git SHA | `ada4a59` |

## 현재 테스트 커버리지

| 영역 | 현재 테스트 | 근거 |
| --- | --- | --- |
| Redis Stream E2E | happy-path 통합 테스트 1건이 producer -> admission -> consumer -> reconciliation 흐름을 커버한다. | [LessonRedisStreamIntegrationTest](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/domain/lesson/issue/LessonRedisStreamIntegrationTest.java:28>) |
| open-run 요청/상태 흐름 | 요청 생성, 중복 거절, 신청 불가 응답, 상태 조회 분기 등은 단위 테스트로 커버된다. | [StudentLessonServiceTest](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonServiceTest.java:297>) |
| 재고 초기화와 승인 가능 수용량 | 레슨 생성 시 open-run 재고 동기화와 승인 흐름에서의 수용량 초과 거절을 단위 테스트가 커버한다. | [AdminLessonServiceTest](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/domain/lesson/teacher/service/AdminLessonServiceTest.java:285>) |
| reconciliation / teacher 스케줄러 | 교사 상태 스케줄러는 시작/완료/no-op 분기에 대한 브랜치 커버리지를 가진다. | [LessonStatusSchedulerTest](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/domain/lesson/teacher/scheduler/LessonStatusSchedulerTest.java:28>) |
| 동시성 / 락 경로 | SpringBootTest 스트레스 테스트가 비관적/분산 락 동작은 커버하지만, 스트림 실패 처리는 커버하지 않는다. | [LessonApplyLockTest](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/domain/lesson/student/LessonApplyLockTest.java:67>) |
| 테스트 인프라 | 레슨 스트림 통합 테스트용 Redis/Postgres Testcontainers 지원이 이미 존재한다. | [RedisStreamIntegrationTestSupport](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/testsupport/RedisStreamIntegrationTestSupport.java:22>) |

## 테스트 공백

1. 재고 소진과 중복 신청은 서비스 경계에서만 검증되고, `LessonApplyProducer.send`에서 실제로 일어나는 Redis 상태 변경은 검증되지 않는다.
   - `StudentLessonServiceTest`는 `ALREADY_APPLIED`와 `LESSON_NOT_AVAILABLE` 매핑을 증명하지만, Redis set 롤백, stock 증가 롤백, waiting room enqueue 억제는 검증하지 않는다.
2. consumer 실패 처리에 대한 집중 테스트가 없다.
   - `LessonApplyConsumer.processIndividually`는 예외 발생 시 레코드를 pending에 남기고, `LessonPendingMessageRecoveryScheduler.recoverPendingMessages`는 10초 후 stale pending 항목을 claim하지만, 두 경로 모두 테스트되지 않았다.
3. admission 실패 시 재등록이 테스트되지 않는다.
   - `LessonAdmissionScheduler.processAdmissionForLesson`은 `multiGet`이 `null`이거나 pipeline이 예외를 던질 때 요청을 재등록하지만, 현재 테스트 suite는 `LessonWaitingRoomService.requeueAfterAdmissionFailure`가 요청을 waiting room으로 되돌리는지 검증하지 않는다.
4. DB 쓰기 실패 의미론이 테스트되지 않는다.
   - `LessonApplyService.apply`는 `BusinessException`에 대해서만 `FAIL:*`을 기록하고 일반 예외는 다시 던진다. `lessonParticipantRepository.save`나 `jdbcTemplate.batchUpdate`가 실패할 때 status/Redis 동작을 고정하는 테스트가 없다.
5. reconciliation 중복 구간 경계가 테스트되지 않는다.
   - `LessonStockReconciliationScheduler.reconcileStock`는 waiting room에 메시지가 남아 있거나, stream에 backlog/pending 항목이 있거나, busy 카운터가 stale/negative일 때 재고 정산을 미뤄야 한다. 이 가드들은 아직 아무 것도 단언되지 않았다.

## 고위험 회귀 후보

| 우선순위 | 후보 | 위험한 이유 | 권장 테스트 유형 |
| --- | --- | --- | --- |
| P1 | consumer 시스템 실패 후 레코드가 pending에 남아 있고 recovery가 이를 유지해야 함 | `LessonApplyService.apply`에서 예외가 발생하면 현재 ACK/XDEL을 건너뛰고 pending recovery에 의존한다. 이 경로가 회귀되면 요청이 유실되거나 중복 처리될 수 있다. | 통합 |
| P1 | admission 실패 시 waiting-room 요청을 반드시 재등록해야 함 | `LessonAdmissionScheduler`는 MQ에 쓰기 전에 ZSET에서 요청을 꺼낸다. requeue가 실패하면 요청이 두 곳 모두에서 사라진다. | 통합 |
| P1 | backlog/busy 상태가 존재하는 동안 reconciliation은 미뤄져야 함 | `LessonStockReconciliationScheduler`에는 waiting-room backlog, stream lag, busy counter를 위한 명시적 가드가 있다. 이 가드가 깨지면 재고와 참가자 수가 오염될 수 있다. | 통합 |
| P2 | DB 쓰기 실패는 의도한 status 의미론을 유지해야 함 | consumer 계층은 `BusinessException`(`FAIL:*`)과 시스템 예외(pending)를 구분한다. 이 경계는 쉽게 회귀되고 Redis/DB 상태를 모호하게 만들 수 있다. | 단위 + 통합 |
| P2 | 재고 소진 롤백은 원자성을 유지해야 함 | `LessonApplyProducer.send`는 stock을 감소시키고, underflow에서 롤백하며, duplicate key를 제거한다. 작은 분기 버그만 있어도 유령 예약이 생길 수 있다. | 단위 |
| P3 | 중복 신청은 Redis 경계에서 멱등성을 유지해야 함 | 서비스 목 테스트는 `ALREADY_APPLIED`를 커버하지만, 실제 Redis set 의미론과 waiting-room 부수 효과는 고정되지 않았다. | 단위 + 통합 |

## 단위 테스트와 통합 테스트 경계

| 경계 | 여기에 들어갈 것 | 이유 |
| --- | --- | --- |
| 단위 테스트 | `LessonApplyProducer.send` 분기, `LessonWaitingRoomService.requeueAfterAdmissionFailure`, `LessonApplyService.apply`/`applyBatch` 실패 분기, 목 처리된 Redis/JDBC를 사용하는 `LessonStockReconciliationScheduler` helper 가드 | 이 경로들은 대부분 분기 로직이고, RedisTemplate / repository / JDBC를 목으로 대체해도 검증할 수 있다. |
| 통합 테스트 | producer -> admission -> consumer -> pending recovery -> reconciliation 흐름, stream pending/claim/ack/delete 동작, request 재등록, 실패 후 Redis/Postgres 일관성 | 이 동작들은 실제 Redis Stream 의미론, consumer group 상태, DB write에 의존한다. Testcontainers로 검증해야 한다. |

## Testcontainers 적용성

| 상태 | 설명 |
| --- | --- |
| 필요함 | `pending`, `claim`, `acknowledge`, `delete`, ZSET dequeue/requeue, Redis/Postgres 일관성에 의존하는 Redis Stream 실패 케이스에는 필요하다. |
| 이미 준비됨 | `RedisStreamIntegrationTestSupport`는 이미 Redis와 PostgreSQL 컨테이너를 시작하고 테스트 프로퍼티를 주입한다. `build.gradle`에는 이미 `org.testcontainers:junit-jupiter`와 `org.testcontainers:postgresql`이 포함되어 있다. |
| 불필요함 | `StudentLessonServiceTest`, DTO/mapper 테스트, 목으로 충분한 scheduler helper 로직 같은 순수 서비스 단위 분기에는 불필요하다. |

## 연구 결론

레슨 도메인에는 Redis Stream happy-path 통합 테스트 1건과 서비스 분기 단위 테스트 몇 개가 있지만, 요청 유실과 재고 일관성에 가장 중요한 실패 경로는 아직 고정되지 않았다. 빠진 경로는 consumer pending-recovery 경로, admission 실패 requeue, DB 쓰기 실패 의미론, reconciliation 중복 구간 가드다.

Planner는 이 실패 경로들 중 무엇을 먼저 단위 테스트로 둘지, 무엇을 Testcontainers 기반 통합 테스트로 둘지 결정해야 한다. 현재 인프라는 이미 Redis/Postgres 통합 테스트를 지원하므로, 핵심 질문은 설정이 아니라 테스트 범위와 우선순위다.

## 근거 파일 목록

- [build.gradle](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/build.gradle:26>)
- [RedisStreamIntegrationTestSupport](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/testsupport/RedisStreamIntegrationTestSupport.java:22>)
- [LessonRedisStreamIntegrationTest](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/domain/lesson/issue/LessonRedisStreamIntegrationTest.java:28>)
- [LessonApplyProducer](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/main/java/com/threestar/trainus/domain/lesson/issue/LessonApplyProducer.java:24>)
- [LessonWaitingRoomService](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/main/java/com/threestar/trainus/domain/lesson/issue/LessonWaitingRoomService.java:27>)
- [LessonAdmissionScheduler](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/main/java/com/threestar/trainus/domain/lesson/issue/LessonAdmissionScheduler.java:37>)
- [LessonApplyConsumer](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/main/java/com/threestar/trainus/domain/lesson/issue/LessonApplyConsumer.java:42>)
- [LessonPendingMessageRecoveryScheduler](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/main/java/com/threestar/trainus/domain/lesson/issue/LessonPendingMessageRecoveryScheduler.java:41>)
- [LessonStockReconciliationScheduler](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/main/java/com/threestar/trainus/domain/lesson/issue/LessonStockReconciliationScheduler.java:41>)
- [StudentLessonServiceTest](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonServiceTest.java:297>)
- [AdminLessonServiceTest](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/domain/lesson/teacher/service/AdminLessonServiceTest.java:285>)
- [LessonStatusSchedulerTest](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/domain/lesson/teacher/scheduler/LessonStatusSchedulerTest.java:28>)
- [LessonApplyLockTest](</Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/domain/lesson/student/LessonApplyLockTest.java:67>)
