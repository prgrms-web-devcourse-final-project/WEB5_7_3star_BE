# Test Plan

## Core Domain Branch Coverage Plan - 2026-06-13

### 실행 정보

```text
실행 일시: 2026-06-13
사용 모델: gpt-5.4-mini
reasoning effort: high
에이전트 역할: Planner
사용 프롬프트: core-domain branch coverage planning
참조 문서:
- docs/ai-test-workflow/AGENTS.md
- docs/ai-test-workflow/rules/agent-roles.md
- docs/ai-test-workflow/rules/human-approval-policy.md
- docs/ai-test-workflow/rules/test-boundary.md
- docs/ai-test-workflow/rules/enforcement-checklist.md
- docs/ai-test-workflow/prompts/planner.md
- docs/ai-test-workflow/templates/test-plan-template.md
- docs/ai-test-workflow/reports/unit-test-coverage/cycles/core-domain-unit-coverage/research-report.md
작업 주제명: core-domain-unit-coverage
```

### 작업 목표

```text
이번 작업의 목표:
- Researcher가 정리한 core-domain gap을 기준으로 payment/refund/coupon, lesson search, user auth, lesson application, DTO/mapper의 unit-test 우선순위를 다시 정렬한다.
- unit-test-only 범위에서 branch coverage를 높이고, integration-only 후보는 명시적으로 분리한다.

이번 작업에서 하지 않을 것:
- production code, build.gradle, settings, application-test.yml 변경
- Testcontainers, Redis/PostgreSQL integration test, controller slice test 구현
- consumer/recovery/scheduler/open-run concurrency 경로 구현
- 구현 전 테스트 코드 작성
```

### 테스트 보강 우선순위

| 순서 | 대상 | 테스트 유형 | 목적 | 예상 변경 파일 |
| --- | --- | --- | --- | --- |
| 1 | `PaymentService.preparePayment`, `processConfirm`, `processCancel`, `viewAllSuccessTransaction`, `viewAllFailureTransaction`, `validateDuplicatedPayment` | 단위 | 결제 준비-확정-취소와 성공/실패 조회, 중복 결제 차단 분기를 먼저 고정한다. | `src/test/java/com/threestar/trainus/domain/payment/service/PaymentServiceTest.java` |
| 2 | `CouponService.calculateDiscountedPrice`, `getValidUserCoupon`, `useCoupon`, `restoreCoupon` | 단위 | 퍼센트/정액 할인 계산과 ACTIVE/INACTIVE 상태 가드를 먼저 잠근다. | `src/test/java/com/threestar/trainus/domain/coupon/user/service/CouponServiceTest.java` |
| 3 | `StudentLessonService.searchLessons`, `searchLessonsByLocation`, `getMyLessonApplications`, `getAsyncApplyStatus`, `LessonSearchMapper`, `LessonApplicationMapper` | 단위 | page/limit/sort/ALL/검색 조건과 신청 목록/상태 조회 분기를 고정한다. | `src/test/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonServiceTest.java`, `src/test/java/com/threestar/trainus/domain/lesson/student/mapper/LessonSearchMapperTest.java`, `src/test/java/com/threestar/trainus/domain/lesson/student/mapper/LessonApplicationMapperTest.java` |
| 4 | `UserService.validateUserExists`, `getAdminUser`, `validateAdminRole`, `withdraw`, `LoginUserArgumentResolver` | 단위 | 사용자 존재성, 관리자 권한, 탈퇴 차단, principal 추출의 auth 경계를 고정한다. | `src/test/java/com/threestar/trainus/domain/user/service/UserServiceTest.java`, `src/test/java/com/threestar/trainus/global/resolver/LoginUserArgumentResolverUnitTest.java` |
| 5 | `LessonCreateRequestDto`, `LessonUpdateRequestDto`, `ApplicationActionRequestDto`, `PageRequestDto`, payment/user request DTO validation, `AdminCouponMapper` | 단위 | 입력 계약과 mapper shape 회귀를 낮은 비용으로 고정한다. | DTO/mapper test files listed in approval block |

### 단위 테스트 계획

```text
대상:
- payment/refund branch: `PaymentService`, `CouponService`
- lesson search branch: `StudentLessonService`, `LessonSearchMapper`, `LessonApplicationMapper`
- user auth branch: `UserService`, `LoginUserArgumentResolver`
- lesson application branch: `StudentLessonService`, 필요 시 `AdminLessonService`의 남은 분기
- DTO/mapper branch: request DTO validation과 `AdminCouponMapper`

검증할 동작:
- payment는 prepare/confirm/cancel/ledger 조회와 coupon use/restore 연동 여부를 검증한다.
- coupon은 discount string format, ACTIVE-only guard, restore/use idempotence를 검증한다.
- search는 sort/page/limit/category/all/keyword/location 조합과 응답 shape을 검증한다.
- auth는 user existence, admin guard, withdraw blocking, login principal resolution을 검증한다.
- application은 status parsing, empty list, count wrapping, failure reason branches를 검증한다.
- DTO/mapper는 Bean Validation 제약과 mapping field preservation을 검증한다.

mock/fake 전략:
- 서비스 테스트는 `@ExtendWith(MockitoExtension.class)`와 `@InjectMocks`로 Spring context 없이 실행한다.
- repository, client, producer, redis template, waiting room service, profile service, user service, password encoder, jwt provider는 Mockito mock으로 대체한다.
- payment/coupon/search/auth는 local builder fixture와 private factory method를 우선 사용하고, 공통 support class는 이번 사이클에서 만들지 않는다.
- 시간 의존 분기에는 고정된 `LocalDateTime` fixture를 쓰고, validation 테스트는 `jakarta.validation.Validator`로 constraint violation을 직접 검사한다.
- mapper 테스트는 minimal object graph와 고정 리스트만 사용하고, repository/Redis 호출은 하지 않는다.
```

### 통합 테스트 경계

```text
대상:
- Redis/PostgreSQL이 실제 필요한 lesson issue 경로
- consumer/recovery/scheduler/open-run concurrency
- repository query correctness
- controller slice / security filter 흐름

필요 인프라:
- Redis, PostgreSQL, Testcontainers 후보

Testcontainers 적용 여부:
- 이번 계획에서는 적용하지 않는다.
- unit-test-only reinforcement가 목표이므로 외부 인프라 검증은 후속 통합 사이클로 넘긴다.

데이터 setup:
- 없음

데이터 cleanup:
- 없음

경계 원칙:
- 단위 테스트는 branch/validation/mapper 계약만 고정한다.
- 외부 인프라 결과가 바뀌는 경로는 통합 전용으로 남긴다.
```

### 우선 구현 순서

1. `PaymentServiceTest`와 `CouponServiceTest`로 결제/쿠폰 상태 분기를 먼저 고정한다.
2. `StudentLessonServiceTest`와 `LessonSearchMapperTest`로 검색 조건과 신청 목록/상태 조회를 이어서 고정한다.
3. `UserServiceTest`와 `LoginUserArgumentResolverUnitTest`로 권한 경계를 마무리한다.
4. request DTO validation과 `AdminCouponMapperTest`를 추가해 입력 계약과 응답 shape을 잠근다.
5. 남는 Redis/PostgreSQL/consumer/recovery/open-run 경로는 다음 통합 사이클 후보로만 남긴다.

### Human Approval 요청

```text
Purpose:
- Approve a unit-test-only reinforcement cycle for core domains in the prioritized list.

Allowed files:
- src/test/java/com/threestar/trainus/domain/payment/service/PaymentServiceTest.java
- src/test/java/com/threestar/trainus/domain/coupon/user/service/CouponServiceTest.java
- src/test/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonServiceTest.java
- src/test/java/com/threestar/trainus/domain/user/service/UserServiceTest.java
- src/test/java/com/threestar/trainus/global/resolver/LoginUserArgumentResolverUnitTest.java
- src/test/java/com/threestar/trainus/domain/lesson/student/mapper/LessonSearchMapperTest.java
- src/test/java/com/threestar/trainus/domain/lesson/student/mapper/LessonApplicationMapperTest.java
- src/test/java/com/threestar/trainus/domain/coupon/admin/mapper/AdminCouponMapperTest.java
- src/test/java/com/threestar/trainus/domain/lesson/teacher/dto/LessonCreateRequestDtoTest.java
- src/test/java/com/threestar/trainus/domain/lesson/teacher/dto/LessonUpdateRequestDtoTest.java
- src/test/java/com/threestar/trainus/domain/lesson/teacher/dto/ApplicationActionRequestDtoTest.java
- src/test/java/com/threestar/trainus/global/dto/PageRequestDtoTest.java
- src/test/java/com/threestar/trainus/domain/payment/dto/PaymentRequestDtoTest.java
- src/test/java/com/threestar/trainus/domain/payment/dto/ConfirmPaymentRequestDtoTest.java
- src/test/java/com/threestar/trainus/domain/payment/dto/CancelPaymentRequestDtoTest.java
- src/test/java/com/threestar/trainus/domain/user/dto/LoginRequestDtoTest.java
- src/test/java/com/threestar/trainus/domain/user/dto/SignupRequestDtoTest.java
- src/test/java/com/threestar/trainus/domain/user/dto/PasswordUpdateDtoTest.java

Forbidden files:
- src/main/java/**
- build.gradle
- settings.gradle
- src/test/resources/application-test.yml
- src/test/java/com/threestar/trainus/domain/lesson/issue/**
- src/test/java/com/threestar/trainus/domain/lesson/*/controller/**
- src/test/java/com/threestar/trainus/domain/lesson/*/repository/**
- any integration-test-only files or Testcontainers setup
- docs/ai-test-workflow/reports/unit-test-coverage/cycles/core-domain-unit-coverage/research-report.md
- docs/ai-test-workflow/reports/unit-test-coverage/cycles/core-domain-unit-coverage/cycle-summary.md
- docs/ai-test-workflow/reports/unit-test-coverage/cycles/core-domain-unit-coverage/review-report.md

No external deps:
- No Testcontainers
- No new libraries
- No network/download steps

What will not be done:
- No production code changes
- No controller slice tests
- No Redis/PostgreSQL integration tests
- No scheduler/consumer/recovery/concurrency implementation
- No build or profile changes
```

### 검증 명령

```text
- `./gradlew test --tests "com.threestar.trainus.domain.payment.service.PaymentServiceTest" --tests "com.threestar.trainus.domain.coupon.user.service.CouponServiceTest"`
- `./gradlew test --tests "com.threestar.trainus.domain.lesson.student.service.StudentLessonServiceTest" --tests "com.threestar.trainus.domain.lesson.student.mapper.LessonSearchMapperTest" --tests "com.threestar.trainus.domain.lesson.student.mapper.LessonApplicationMapperTest"`
- `./gradlew test --tests "com.threestar.trainus.domain.user.service.UserServiceTest" --tests "com.threestar.trainus.global.resolver.LoginUserArgumentResolverUnitTest"`
- `./gradlew test --tests "com.threestar.trainus.domain.lesson.teacher.dto.LessonCreateRequestDtoTest" --tests "com.threestar.trainus.domain.lesson.teacher.dto.LessonUpdateRequestDtoTest" --tests "com.threestar.trainus.domain.lesson.teacher.dto.ApplicationActionRequestDtoTest" --tests "com.threestar.trainus.global.dto.PageRequestDtoTest" --tests "com.threestar.trainus.domain.payment.dto.PaymentRequestDtoTest" --tests "com.threestar.trainus.domain.payment.dto.ConfirmPaymentRequestDtoTest" --tests "com.threestar.trainus.domain.payment.dto.CancelPaymentRequestDtoTest" --tests "com.threestar.trainus.domain.user.dto.LoginRequestDtoTest" --tests "com.threestar.trainus.domain.user.dto.SignupRequestDtoTest" --tests "com.threestar.trainus.domain.user.dto.PasswordUpdateDtoTest" --tests "com.threestar.trainus.domain.coupon.admin.mapper.AdminCouponMapperTest"`
- full `./gradlew test` is not a required gate for this unit-only plan because broader integration-like tests remain out of scope
```
