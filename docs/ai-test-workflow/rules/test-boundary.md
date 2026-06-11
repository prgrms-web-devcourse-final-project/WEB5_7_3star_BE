# Test Boundary

이 문서는 TrainUs 프로젝트의 테스트 종류와 적용 기준을 정의한다.

목표는 테스트를 많이 만드는 것이 아니라, 테스트 목적에 맞는 실행 경계를 유지하는 것이다.

## 단위 테스트

목적:
- 도메인 로직, 분기, 예외 처리 검증
- 외부 인프라 없이 빠르게 실행

기준:
- Spring context를 가능하면 띄우지 않는다.
- Redis, PostgreSQL, S3 같은 외부 인프라에 직접 연결하지 않는다.
- Repository, RedisTemplate 등은 mock 또는 fake로 대체한다.
- 클래스명은 `*UnitTest`를 사용한다.

적합한 대상:
- 서비스 메서드의 분기 로직
- 검증 로직
- 예외 변환 로직
- DTO mapper 또는 계산 로직

## 통합 테스트

목적:
- Spring context, DB, Redis, transaction, scheduler 등 실제 연결 경로 검증
- 외부 의존성이 있는 회귀 시나리오 고정

기준:
- 클래스명은 `*IntegrationTest`를 사용한다.
- Redis/PostgreSQL이 필요한 경우 Testcontainers 적용을 우선 검토한다.
- 테스트 데이터 setup과 cleanup 전략을 명시한다.
- 운영 profile을 사용하지 않는다.

적합한 대상:
- Redis Stream Consumer 처리
- Waiting Room 상태 조회
- 재고 보정 스케줄러
- PostgreSQL 검색 쿼리
- 인증 사용자 조회와 Security filter 흐름

## 회귀 테스트

목적:
- 과거에 실제로 발생했거나 포트폴리오에서 다룬 문제를 다시 깨지지 않게 고정

우선 후보:
- 중복 신청이 DB에 한 번만 반영되는지
- Consumer 시스템 실패 시 ACK / XDEL하지 않는지
- Admission 실패 시 requestId가 유실되지 않는지
- 보정 스케줄러가 처리 중 상태를 오판하지 않는지
- 지역/위치 검색이 기존 필터와 함께 정상 동작하는지

## Testcontainers 적용 기준

적용하면 좋은 경우:
- Redis Stream, Sorted Set, TTL 같은 Redis 동작 자체가 검증 대상인 경우
- PostgreSQL index, transaction, constraint, native query가 검증 대상인 경우
- 로컬 DB/Redis 상태에 따라 테스트 결과가 달라질 수 있는 경우

피해야 하는 경우:
- 단순 서비스 분기 테스트
- mock으로 충분한 validation 테스트
- 외부 인프라 동작이 검증 대상이 아닌 테스트

