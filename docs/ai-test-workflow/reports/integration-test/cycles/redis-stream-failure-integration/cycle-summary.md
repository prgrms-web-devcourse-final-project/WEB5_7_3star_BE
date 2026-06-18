# 레슨 Redis Stream 실패 케이스 - 사이클 요약

## 범위

- 작업 흐름: `integration-test`
- 사이클: `redis-stream-failure-integration`
- 승인된 구현 범위:
  - 레슨 도메인만
  - Redis Stream 실패 중심 통합 테스트
  - 운영 코드 변경 없음
  - 쿠폰 도메인 변경 없음

## 구현된 테스트

- `LessonRedisStreamFailureIntegrationTest`
  - producer 중복 거절이 Redis stock / waiting-room 상태를 안정적으로 유지하는지 검증
  - producer 재고 소진이 Redis pre-filter 상태를 되돌리고 queue를 비우는지 검증
  - admission status 조회 실패 시 dequeued lesson request를 재등록하는지 검증
  - admission pipeline 실패 시 dequeued lesson request를 재등록하는지 검증

## 인프라

- `RedisStreamIntegrationTestSupport`
  - Redis/PostgreSQL Testcontainers 기반 통합 지원이 이미 존재했고 재사용했다.
- `build.gradle`
  - 이번 사이클에서 변경 없음
- `application-test.yml`
  - 이번 사이클에서 변경 없음

## 검증

```text
Command 1:
./gradlew test --tests "com.threestar.trainus.domain.lesson.issue.LessonRedisStreamFailureIntegrationTest"

Result:
BUILD SUCCESSFUL

Command 2:
./gradlew test --tests "com.threestar.trainus.domain.lesson.issue.LessonRedisStreamIntegrationTest" --tests "com.threestar.trainus.domain.lesson.issue.LessonRedisStreamFailureIntegrationTest"

Result:
BUILD SUCCESSFUL
```

참고:

- 테스트는 실제 Redis와 PostgreSQL 컨테이너를 사용했다.
- 빌드 종료 후 Redis/PostgreSQL 연결 해제 경고가 출력됐지만 빌드는 실패하지 않았다.

## 리뷰 결과

- Reviewer 상태: changes requested.
- 핵심 리뷰 포인트:
  - 승인된 malformed-status admission 케이스가 계획대로 구현되지 않았다.
  - 현재 구현은 그 대신 pipeline-failure requeue 케이스를 다루고 있다.
- 구현된 테스트는 승인된 레슨 도메인 경계 안에 머무른다.

## 후속 후보

- malformed-status admission 테스트는 production path가 malformed payload 보존을 지원하도록 바뀌는 경우에만 추가한다.
- consumer pending-recovery coverage는 다음 사이클로 미룬다.
- ACK/XDEL 실패와 reconciliation safety gate coverage도 다음 사이클로 미룬다.
