# Project-Wide Domain Integration Gaps - Research Report

## 실행 정보

```text
실행 시각: 2026-06-15 17:02:18 KST
사용 모델: gpt-5.4-mini
에이전트 역할: Researcher
사용 프롬프트: docs/ai-test-workflow/prompts/researcher.md
참조 문서: docs/ai-test-workflow/AGENTS.md, docs/ai-test-workflow/rules/agent-roles.md, docs/ai-test-workflow/rules/human-approval-policy.md, docs/ai-test-workflow/rules/test-boundary.md, docs/ai-test-workflow/rules/enforcement-checklist.md, docs/ai-test-workflow/templates/research-report-template.md
조사 범위: src/test 테스트 현황, build.gradle 테스트 설정, test profile 및 외부 의존성, Redis/PostgreSQL/Scheduler/Consumer/Security 경로, 프로젝트 전체 고위험 통합 테스트 후보
실행 메모: 테스트 실행 없음, 운영 코드 변경 없음, 테스트 코드 변경 없음
```

## 1. 현재 존재하는 테스트

| 영역 | 현재 상태 | 근거 |
| --- | --- | --- |
| 단위 테스트 | 서비스, mapper, DTO, entity, resolver 중심의 Mockito/JUnit 테스트가 넓게 존재한다. | `UserServiceTest`, `CouponServiceTest`, `AdminCouponServiceTest`, `ProfileMetadataServiceTest`, `AdminLessonServiceTest`, `LessonCreationLimitServiceTest`, `PaymentServiceTest`, `LoginUserArgumentResolverUnitTest`, `LessonStatusSchedulerTest`, `CouponStatusSchedulerTest` |
| 통합 성격 테스트 | Spring context, MockMvc, 실제 DB/Redis 접근, 동시성 부하 성격의 테스트가 일부 있다. | `CommentControllerTest`, `ReviewControllerTest`, `TrainUsApplicationTests`, `LessonApplyLockTest`, `UserCouponServiceConcurrencyTests`, `UserCouponServiceRedissonLockTests`, `LessonDatabaseSetupTest`, `LessonSearchPerformanceTest` |
| 비활성/주석 처리 테스트 | 파일은 존재하지만 전체가 주석 처리되어 실행되지 않는다. | `UserCouponControllerTests`, `KeywordSearchPerformanceTest`, `LocationSearchPerformanceTest` |
| 경계 불일치 | 문서 기준 `*IntegrationTest` 네이밍이 없고, 통합성 테스트도 `*Test`/`*Tests`로 혼재한다. | `src/test/java` 전체 탐색 결과 |

현 상태는 단위 테스트 수가 많고, 실제 인프라를 쓰는 통합성 테스트는 일부 있지만 대부분이 불안정한 환경 의존 또는 성능/부하 성격이다. `LessonSearchPerformanceTest`와 `LessonDatabaseSetupTest`는 DB를 실제로 건드리지만 검증용 회귀 테스트라기보다 성능/데이터 생성 도구에 가깝다.

## 2. 테스트가 부족한 경로

1. Redis Stream 기반 레슨 신청 경로의 end-to-end 검증이 없다. `LessonApplyProducer`, `LessonAdmissionScheduler`, `LessonApplyConsumer`, `LessonPendingMessageRecoveryScheduler`, `LessonStockReconciliationScheduler`가 함께 맞물리지만 이 조합을 실제 Redis와 DB로 검증하는 테스트가 없다.
2. 쿠폰 발급의 Redis 재고 차감, Stream enqueue, consumer ACK/rollback 경로가 실제 인프라 기준으로 묶여 있지 않다. 현재는 동시성 부하 테스트만 있고 consumer/pending 처리까지 이어지는 회귀 테스트가 없다.
3. PostgreSQL 쿼리와 PostGIS 검색 경로는 존재하지만, `LessonRepository`, `ReviewRepository`, `CouponRepository`, `UserCouponRepository`, `ProfileMetadataRepository`의 실제 query correctness를 보장하는 통합 테스트가 부족하다.
4. Security filter chain과 JWT 인증 경로가 통합 테스트로 검증되지 않는다. 현재 HTTP 테스트는 `MockHttpSession`의 `LOGIN_USER` 주입에 의존하는 편이라 `JwtAuthenticationFilter`를 통과하는 실제 흐름이 없다.
5. 스케줄러는 Mockito 기반 단위 테스트가 중심이라 `@Scheduled`, `@Transactional`, ShedLock, repository query, Redis/DB side effect를 함께 검증하지 못한다.
6. test profile이 self-contained하지 않다. `src/test/resources/application-test.yml`은 PostgreSQL/Redis/AWS/Payment 관련 env var를 계속 요구하고, `RedisConfig`의 `spring.data.redis.core` / `spring.data.redis.mq` prefix와도 맞지 않는다. 추정이지만 현재 test profile은 외부 환경 없이는 안정적으로 뜨기 어렵다.

## 3. 단위 테스트와 통합 테스트 경계상 통합 테스트로 넘길 후보

| 후보 | 이유 |
| --- | --- |
| `LessonApplyConsumer`, `LessonAdmissionScheduler`, `LessonPendingMessageRecoveryScheduler`, `LessonStockReconciliationScheduler` | Core Redis, MQ Redis, PostgreSQL, scheduler, lock, recovery가 함께 작동하는 핵심 회귀 경로다. |
| `CouponIssueProducer`, `CouponIssueConsumer`, `CouponStatusScheduler` | 쿠폰 재고 차감, Stream enqueue, consumer ACK/rollback, 상태 갱신이 하나의 도메인 흐름이다. |
| `SecurityConfig`, `JwtAuthenticationFilter` | 권한 없는 접근, 관리자 role, 인증 실패/성공을 실제 filter chain으로 확인해야 한다. |
| `LessonRepository`, `ReviewRepository`, `ProfileMetadataRepository`, `CouponRepository`, `UserCouponRepository`의 custom query 경로 | 실제 PostgreSQL/PostGIS에서 쿼리 결과와 페이징, count, transactional side effect를 확인해야 한다. |
| `ProfileMetadataScheduler` | 리뷰 집계와 메타데이터 갱신이 DB 기반으로 실제 반영되는지 확인해야 한다. |
| `CommentControllerTest`, `ReviewControllerTest`의 JWT 기반 인증 경로 | 현재는 세션 주입 중심이라 실제 인증 경로 회귀를 못 잡는다. |

단위 테스트로 남기기 좋은 영역은 DTO, mapper, entity invariant, 그리고 외부 인프라 없이 분기만 검증하는 서비스 메서드다. 이 프로젝트에서는 `*MapperTest`, `*DtoTest`, `*EntityTest`, `UserServiceTest`, `CouponServiceTest`, `PaymentServiceTest`, `LessonCreationLimitServiceTest`, `ProfileMetadataServiceTest` 같은 파일이 여기에 해당한다.

## 4. 우선순위 높은 통합 테스트 후보

| 우선순위 | 대상 | 필요한 이유 | 테스트 유형 |
| --- | --- | --- | --- |
| P1 | 레슨 Redis Stream 신청 end-to-end | 선착순 신청의 핵심 경로이며 Redis core/mq, DB, consumer, recovery, scheduler가 모두 엮인다. 현재는 stress test만 있고 회귀 보장이 없다. | `*IntegrationTest` |
| P1 | 쿠폰 발급 Stream end-to-end | 재고 차감, 메시지 발행, consumer ACK, pending 복구, 롤백이 함께 맞물린다. | `*IntegrationTest` |
| P1 | JWT 보안 필터와 관리자 권한 경로 | `SecurityConfig`와 `JwtAuthenticationFilter`가 실제로 인증/인가를 강제하는지 확인해야 한다. | `*IntegrationTest` |
| P2 | PostgreSQL/PostGIS 레슨 검색 쿼리 | LIKE, full-text, location, distance 쿼리는 회귀 위험이 크고 성능 테스트만으로는 correctness를 보장하지 못한다. | `*IntegrationTest` |
| P2 | 레슨 재고 보정 스케줄러 | DB count를 기준으로 lesson row와 Redis stock을 맞추는 경로이며 ShedLock까지 포함된다. | `*IntegrationTest` |
| P2 | 프로필 메타데이터 스케줄러 | 리뷰 count/rating 집계가 실제 DB와 일치하는지 검증해야 한다. | `*IntegrationTest` |
| P3 | 댓글/리뷰 HTTP 인증 흐름 | Mock session 대신 JWT 기반 인증과 권한 흐름을 고정해야 한다. | `*IntegrationTest` |
| P3 | 결제/S3 경계 테스트 | 외부 API와 파일 업로드 경계는 별도 후보로 관리하는 편이 낫다. | 별도 후보 |

참고로 `LessonApplyLockTest`, `UserCouponServiceConcurrencyTests`, `UserCouponServiceRedissonLockTests`는 현재도 부하/동시성 성격의 통합성 테스트처럼 보이지만, 실제 consumer, recovery, JWT, scheduler, DB query correctness까지는 덮지 못한다.

## 5. Redis/PostgreSQL/Scheduler/Consumer/Security 관련 공백

| 영역 | 공백 | 근거 |
| --- | --- | --- |
| Redis | Testcontainers나 embedded Redis가 없고, test profile도 RedisConfig의 core/mq prefix와 맞지 않는다. | `build.gradle`, `src/test/resources/application-test.yml`, `src/main/java/com/threestar/trainus/global/config/RedisConfig.java` |
| PostgreSQL | test profile이 외부 DB env var에 의존하고, PostGIS 쿼리는 성능 테스트로만 노출된다. | `src/test/resources/application-test.yml`, `LessonSearchPerformanceTest`, `LessonDatabaseSetupTest` |
| Scheduler | 실제 스케줄링 실행이 아니라 repository mock 검증 위주다. | `LessonStatusSchedulerTest`, `CouponStatusSchedulerTest` |
| Consumer | consumer profile 기반의 Stream 처리, ACK, delete, pending recovery, requeue 경로가 테스트되지 않았다. | `CouponIssueConsumer`, `LessonApplyConsumer`, `LessonPendingMessageRecoveryScheduler`, `LessonAdmissionScheduler` |
| Security | filter chain과 JWT 인증/role enforcement 통합 검증이 없다. | `SecurityConfig`, `JwtAuthenticationFilter`, `CommentControllerTest`, `ReviewControllerTest` |

## 6. Testcontainers 적용 검토

```text
필요 여부: 필요
적용 후보: PostgreSQL/PostGIS 통합 테스트, Redis core/mq 통합 테스트, consumer/scheduler end-to-end 경로, repository query correctness 검증
적용하지 않아도 되는 영역: DTO, mapper, entity, 순수 분기용 서비스 단위 테스트
이유: 현재 test profile이 외부 env var에 의존하고, RedisConfig prefix와 test profile이 맞지 않으며, PostGIS/Redis 동작을 H2나 mock으로는 대체하기 어렵다.
```

추정이지만 Testcontainers가 없으면 현재 구조는 로컬 Redis/PostgreSQL 또는 CI 환경 변수에 강하게 묶인다. 특히 `LessonSearchPerformanceTest` 계열은 PostGIS 성격이 있어 H2 대체가 맞지 않는다.

## 7. 조사한 파일 목록과 근거

### Workflow 문서

```text
docs/ai-test-workflow/AGENTS.md
docs/ai-test-workflow/rules/agent-roles.md
docs/ai-test-workflow/rules/human-approval-policy.md
docs/ai-test-workflow/rules/test-boundary.md
docs/ai-test-workflow/rules/enforcement-checklist.md
docs/ai-test-workflow/prompts/researcher.md
docs/ai-test-workflow/templates/research-report-template.md
```

### Build 및 profile 설정

```text
build.gradle
src/main/resources/application.yml
src/main/resources/application-local.yml
src/test/resources/application-test.yml
```

### Core main files

```text
src/main/java/com/threestar/trainus/global/config/security/SecurityConfig.java
src/main/java/com/threestar/trainus/global/config/security/JwtAuthenticationFilter.java
src/main/java/com/threestar/trainus/global/resolver/LoginUserArgumentResolver.java
src/main/java/com/threestar/trainus/global/config/RedisConfig.java
src/main/java/com/threestar/trainus/global/config/redis/RedisStreamConfig.java
src/main/java/com/threestar/trainus/domain/lesson/issue/LessonApplyProducer.java
src/main/java/com/threestar/trainus/domain/lesson/issue/LessonApplyConsumer.java
src/main/java/com/threestar/trainus/domain/lesson/issue/LessonAdmissionScheduler.java
src/main/java/com/threestar/trainus/domain/lesson/issue/LessonPendingMessageRecoveryScheduler.java
src/main/java/com/threestar/trainus/domain/lesson/issue/LessonStockReconciliationScheduler.java
src/main/java/com/threestar/trainus/domain/lesson/issue/LessonWaitingRoomService.java
src/main/java/com/threestar/trainus/domain/coupon/issue/CouponIssueProducer.java
src/main/java/com/threestar/trainus/domain/coupon/issue/CouponIssueConsumer.java
src/main/java/com/threestar/trainus/domain/coupon/admin/scheduler/CouponStatusScheduler.java
src/main/java/com/threestar/trainus/domain/lesson/teacher/scheduler/LessonStatusScheduler.java
src/main/java/com/threestar/trainus/domain/metadata/scheduler/ProfileMetadataScheduler.java
src/main/java/com/threestar/trainus/domain/lesson/teacher/repository/LessonRepository.java
src/main/java/com/threestar/trainus/domain/review/repository/ReviewRepository.java
src/main/java/com/threestar/trainus/domain/metadata/repository/ProfileMetadataRepository.java
src/main/java/com/threestar/trainus/domain/coupon/user/repository/CouponRepository.java
src/main/java/com/threestar/trainus/domain/coupon/user/repository/UserCouponRepository.java
```

### Test files reviewed

```text
src/test/java/com/threestar/trainus/TrainUsApplicationTests.java
src/test/java/com/threestar/trainus/coupon/user/UserCouponControllerTests.java
src/test/java/com/threestar/trainus/coupon/user/UserCouponServiceConcurrencyTests.java
src/test/java/com/threestar/trainus/coupon/user/UserCouponServiceRedissonLockTests.java
src/test/java/com/threestar/trainus/coupon/user/UserCouponServiceTests.java
src/test/java/com/threestar/trainus/domain/comment/controller/CommentControllerTest.java
src/test/java/com/threestar/trainus/domain/coupon/admin/mapper/AdminCouponMapperTest.java
src/test/java/com/threestar/trainus/domain/coupon/admin/scheduler/CouponStatusSchedulerTest.java
src/test/java/com/threestar/trainus/domain/coupon/admin/service/AdminCouponServiceTest.java
src/test/java/com/threestar/trainus/domain/coupon/user/service/CouponServiceTest.java
src/test/java/com/threestar/trainus/domain/lesson/student/KeywordSearchPerformanceTest.java
src/test/java/com/threestar/trainus/domain/lesson/student/LessonApplyLockTest.java
src/test/java/com/threestar/trainus/domain/lesson/student/LessonDatabaseSetupTest.java
src/test/java/com/threestar/trainus/domain/lesson/student/LessonSearchPerformanceTest.java
src/test/java/com/threestar/trainus/domain/lesson/student/LocationSearchPerformanceTest.java
src/test/java/com/threestar/trainus/domain/lesson/student/mapper/LessonApplicationMapperTest.java
src/test/java/com/threestar/trainus/domain/lesson/student/mapper/LessonApplyMapperTest.java
src/test/java/com/threestar/trainus/domain/lesson/student/mapper/LessonSearchMapperTest.java
src/test/java/com/threestar/trainus/domain/lesson/student/mapper/LessonSimpleMapperTest.java
src/test/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonServiceTest.java
src/test/java/com/threestar/trainus/domain/lesson/teacher/dto/ApplicationActionRequestDtoTest.java
src/test/java/com/threestar/trainus/domain/lesson/teacher/dto/LessonCreateRequestDtoTest.java
src/test/java/com/threestar/trainus/domain/lesson/teacher/dto/LessonUpdateRequestDtoTest.java
src/test/java/com/threestar/trainus/domain/lesson/teacher/mapper/LessonMapperTest.java
src/test/java/com/threestar/trainus/domain/lesson/teacher/scheduler/LessonStatusSchedulerTest.java
src/test/java/com/threestar/trainus/domain/lesson/teacher/service/AdminLessonServiceTest.java
src/test/java/com/threestar/trainus/domain/lesson/teacher/service/LessonCreationLimitServiceTest.java
src/test/java/com/threestar/trainus/domain/metadata/service/ProfileMetadataServiceTest.java
src/test/java/com/threestar/trainus/domain/payment/dto/CancelPaymentRequestDtoTest.java
src/test/java/com/threestar/trainus/domain/payment/dto/ConfirmPaymentRequestDtoTest.java
src/test/java/com/threestar/trainus/domain/payment/dto/PaymentRequestDtoTest.java
src/test/java/com/threestar/trainus/domain/payment/service/PaymentServiceTest.java
src/test/java/com/threestar/trainus/domain/profile/entity/ProfileTest.java
src/test/java/com/threestar/trainus/domain/review/controller/ReviewControllerTest.java
src/test/java/com/threestar/trainus/domain/user/dto/LoginRequestDtoTest.java
src/test/java/com/threestar/trainus/domain/user/dto/PasswordUpdateDtoTest.java
src/test/java/com/threestar/trainus/domain/user/dto/SignupRequestDtoTest.java
src/test/java/com/threestar/trainus/domain/user/entity/UserTest.java
src/test/java/com/threestar/trainus/domain/user/service/UserServiceTest.java
src/test/java/com/threestar/trainus/global/dto/PageRequestDtoTest.java
src/test/java/com/threestar/trainus/global/resolver/LoginUserArgumentResolverUnitTest.java
src/test/resources/application-test.yml
```

### 비활성 파일

```text
src/test/java/com/threestar/trainus/coupon/user/UserCouponControllerTests.java
src/test/java/com/threestar/trainus/domain/lesson/student/KeywordSearchPerformanceTest.java
src/test/java/com/threestar/trainus/domain/lesson/student/LocationSearchPerformanceTest.java
```

## Researcher 결론

```text
요약: 현재 테스트는 unit 중심으로 넓게 깔려 있지만, Redis Stream consumer/admission/recovery, PostgreSQL/PostGIS query correctness, JWT security filter chain, scheduler transactional side effect에 대한 실제 통합 테스트가 비어 있다. test profile도 외부 env var와 RedisConfig prefix 불일치 때문에 self-contained하지 않다.
다음 단계에서 Planner가 판단해야 할 사항: 어떤 고위험 경로를 Testcontainers 기반 통합 테스트로 우선 전환할지, consumer profile과 test profile을 어떻게 분리할지, performance/seed 파일을 회귀 테스트와 분리해서 유지할지.
```
