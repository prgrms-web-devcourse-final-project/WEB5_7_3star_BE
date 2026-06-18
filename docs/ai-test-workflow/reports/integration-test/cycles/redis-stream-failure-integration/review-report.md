# 리뷰 보고서 - Redis Stream 실패 케이스

## 실행 정보

| 항목 | 값 |
| --- | --- |
| 실행 시각 | 2026-06-16 17:32 KST |
| 역할 | Reviewer |
| 범위 | 레슨 도메인만 |
| 검토한 테스트 | `src/test/java/com/threestar/trainus/domain/lesson/issue/LessonRedisStreamFailureIntegrationTest.java` |

## 상태

수정 요청.

구현은 승인된 레슨 도메인 통합 테스트 경계 안에 대부분 들어가 있고, 지정된 Gradle 명령도 통과했지만, 계획된 하위 케이스 1개가 빠졌다.

## 발견 사항

### 보통 - 승인된 malformed-status admission 케이스가 누락됨

- 근거:
  - `docs/ai-test-workflow/reports/integration-test/cycles/redis-stream-failure-integration/test-plan.md:66-80`에는 이번 사이클에 `admission requeue on malformed status`가 명시되어 있었다.
  - `src/test/java/com/threestar/trainus/domain/lesson/issue/LessonRedisStreamFailureIntegrationTest.java:140-169`는 대신 강제로 `executePipelined(...)` 예외를 발생시키는 테스트를 담고 있다.
  - 현재 production 동작에서 malformed status는 requeue를 호출하지 않는다. `LessonAdmissionScheduler`는 `invalidStatusCount`만 증가시키고 계속 진행하며 `src/main/java/com/threestar/trainus/domain/lesson/issue/LessonAdmissionScheduler.java:96-110`, requeue는 `multiGet == null`이거나 pipeline 예외일 때만 발생한다(`72-77`, `130-133`).
- 영향:
  - 이번 사이클은 승인된 malformed-status request-retention 케이스를 커버했다고 말할 수 없다.
  - 추가된 pipeline-failure 테스트는 유용하고 여전히 레슨 도메인 범위 안이지만, 승인된 동일 케이스의 대체물이다.
- 권고:
  - 이번 사이클에서 malformed-status 통합 assertion을 추가하거나, cycle summary에 malformed-status coverage가 아직 구현되지 않았고 pipeline 예외 coverage가 대신 추가되었다고 명시해야 한다.

## 정확성

- producer 중복 거절은 실제 Redis 상태를 기준으로 올바르게 단언하고 있다. stock은 한 번만 감소하고, duplicate set에는 사용자 1명만 들어가며, waiting room에는 request 1건만 남고, stream 항목은 추가되지 않는다.
- producer 재고 소진은 올바르게 단언하고 있다. request 생성은 `null`을 반환하고, stock은 `0`으로 롤백되며, duplicate key는 제거되고, waiting room은 비어 있고, stream도 비어 있다.
- admission `multiGet == null` 실패는 올바르게 단언하고 있다. dequeue된 request가 재등록되고, dirty set 멤버십이 유지되며, status key는 그대로 남아 있고, stream 항목은 추가되지 않는다.
- admission pipeline 예외 처리도 해당 동작 자체는 올바르게 단언하고 있다. request가 재등록되고 stream은 비어 있다.

## 경계 적합성

- 구현은 승인된 테스트 파일 `LessonRedisStreamFailureIntegrationTest.java` 안에 머문다.
- 운영 코드, build 설정, application-test profile, controller/DTO/mapper/repository 테스트, 다른 도메인은 건드리지 않는다.
- 테스트 유형은 경계와 맞는다. Redis ZSET/set/value/stream 상태를 기존 Testcontainers 기반 통합 지원으로 검증하고 있다.
- consumer pending recovery, ACK/XDEL 실패, stock reconciliation gate는 계획대로 범위 밖이다.

## 플래키 위험

- 낮음에서 보통 수준.
- 실제 Redis/PostgreSQL 컨테이너를 사용하므로 이 통합 경계에는 적절하지만, 더 느리고 Docker 의존적이다.
- requeue assertion은 rank `1`과 waiting-room entry 1건만 요구하므로 timestamp 기반 requeue scoring은 현재 플래키하지 않다.
- 집중 실행은 성공했지만 종료 시 Redis/Postgres 연결 해제 경고가 발생했다. 빌드는 실패하지 않았지만 CI 로그에는 잡음이 될 수 있다.

## 정리

- Redis cleanup은 `RedisStreamIntegrationTestSupport`의 wildcard delete로 각 테스트 뒤에 처리된다.
- DB cleanup은 이 테스트의 fixture 순서에 맞춰 participants, lessons, users를 지운다.
- 이 테스트는 pending stream entry나 consumer group을 만들지 않으므로 stream key 삭제만으로 충분하다.

## 검증

```text
./gradlew test --tests "com.threestar.trainus.domain.lesson.issue.LessonRedisStreamFailureIntegrationTest"
```

결과: `BUILD SUCCESSFUL`

종료 시 PostgreSQL/Hibernate schema-drop과 Redis reconnect 경고가 보였지만, 테스트 실패는 아니었다.

## Reviewer 결론

현재 구현은 레슨 도메인 Redis 실패 통합 테스트의 유용한 조각이며 승인 범위에도 대체로 맞는다. 그러나 malformed-status admission 케이스를 구현하거나, 명시적으로 후속으로 미뤘다고 적기 전까지는 승인된 계획을 완전히 끝냈다고 설명하면 안 된다.
