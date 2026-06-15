# Review Report

## 실행 정보

```text
실행 일시: 2026-06-14 00:51:08 KST
사용 모델: inherited from main agent
에이전트 역할: Reviewer
사용 프롬프트: core-domain-branch-coverage 구현 결과 읽기 전용 검토
참조 문서:
- docs/ai-test-workflow/AGENTS.md
- docs/ai-test-workflow/reports/unit-test-coverage/cycles/core-domain-branch-coverage/test-plan.md
검토 대상:
- payment/coupon/student lesson/user service unit tests
- mapper tests
- DTO validation tests
```

## 검토한 파일

```text
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
```

## 심각도 높은 문제

| 항목 | 위치 | 문제 | 권장 조치 |
| --- | --- | --- | --- |
| 없음 | - | Reviewer가 high severity 문제를 보고하지 않았다. | - |

## 보완하면 좋은 문제

| 항목 | 위치 | 문제 | 조치 결과 |
| --- | --- | --- | --- |
| Medium | `PaymentServiceTest.processCancel_SuccessRestoresCoupon` | 외부 취소 요청 DTO와 환불금 산정값을 검증하지 않아 환불 정책 회귀를 놓칠 수 있었다. | `ArgumentCaptor<TossCancelRequestDto>`로 `paymentKey`, `cancelReason`, `cancelAmount`를 검증하고 `Payment.refundPrice`를 30% 환불액으로 검증하도록 수정했다. |
| Medium | `CouponServiceTest.getValidUserCoupon_*` | `userCouponId` 변수명이 repository의 `findByUserIdAndCouponId` 계약과 혼선을 만들 수 있었다. | 변수와 DisplayName을 `couponId` 기준으로 정리하고 repository 호출 인자를 명시 검증했다. |
| Low | `StudentLessonServiceTest.searchLessons_WithKeywordUsesFullTextAndOffset` | count query의 `countLimit`을 `anyInt()`로 열어 page 기반 계산 회귀를 잡기 어려웠다. | page 2, pageSize 10, movable page 5 기준 `countLimit=51`을 검증하도록 수정했다. |

## 테스트 경계 검토

```text
단위 테스트 경계 위반 여부: 없음
통합 테스트 경계 위반 여부: 없음
Testcontainers 사용 적절성: 사용하지 않음
외부 환경 의존성: 없음
flaky 가능성: 낮음. 시간 의존 결제 취소 테스트는 now 기준 +5일 fixture로 24시간 전 취소 조건과 30% 환불 정책을 만족한다.
```

## 외부 설명 가능 내용

```text
주장 가능한 내용:
- PaymentService prepare/confirm/cancel 일부 분기와 ledger empty/missing TossPayment 분기를 단위 테스트로 고정했다.
- CouponService percent/fixed discount, active guard, use/restore idempotence를 단위 테스트로 고정했다.
- Lesson search full-text/no-keyword, mapper shape, user admin/withdraw/existence, DTO validation 일부를 단위 테스트로 보강했다.

아직 주장하면 안 되는 내용:
- Redis/PostgreSQL query correctness, controller/security filter, scheduler/consumer/recovery/concurrency 경로까지 검증했다는 주장
- full suite 전체 안정성 검증 완료 주장
```

## Reviewer 결론

```text
승인 가능 여부: 보완 지적 3건 반영 후 targeted test 기준 승인 가능
남은 리스크: 통합 인프라와 repository query correctness는 후속 integration cycle 대상으로 남아 있다.
```
