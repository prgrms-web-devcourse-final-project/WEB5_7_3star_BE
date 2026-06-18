# Core Domain Branch Coverage Cycle Summary

## Scope

- `core-domain-unit-coverage` 사이클의 승인 범위 안에서 unit-test-only 보강을 수행했다.
- 대상은 payment/refund/coupon, lesson search/application mapper, user auth/withdraw, request DTO validation, admin coupon mapper였다.
- production code, build/profile 설정, Testcontainers, controller/repository/integration test는 변경하지 않았다.

## Implemented Tests

- `PaymentServiceTest`: 결제 준비, 기존 READY 재사용, 쿠폰 할인, 중복 결제 차단, 승인 성공/실패, 취소 가능 시간, 취소 성공과 쿠폰 복원, 성공 결제 내역 empty/missing TossPayment 분기를 검증했다.
- `CouponServiceTest`: percent/fixed 할인 계산, ACTIVE-only 조회, use/restore idempotence를 검증했다.
- `StudentLessonServiceTest`: 검색어 없는 검색과 full-text 검색의 offset/countLimit/응답 shape을 보강했다.
- `UserServiceTest`: admin guard, user existence, withdraw success/blocking branches를 보강했다.
- Mapper tests: `LessonSearchMapperTest`, `LessonApplicationMapperTest`, `AdminCouponMapperTest`로 응답 shape과 nested field mapping을 검증했다.
- DTO validation tests: lesson teacher DTO, global page request, payment request, user request validation contracts를 검증했다.

## Validation

- 실행 명령: targeted `./gradlew test` with approved `--tests` list
- 결과: `BUILD SUCCESSFUL`
- 실행 시각: 2026-06-14 00:51 KST

## Review Result

- Reviewer가 `PaymentServiceTest`, `CouponServiceTest`, `StudentLessonServiceTest`의 테스트 정밀도 문제 3건을 보고했다.
- 결제 취소 테스트는 `TossCancelRequestDto` 캡처와 30% 환불금 검증으로 보완했다.
- 쿠폰 조회 테스트는 `couponId` 계약이 드러나도록 명명과 repository 호출 검증을 보완했다.
- lesson search 테스트는 `countLimit=51`을 명시 검증하도록 보완했다.
- 보완 후 동일 targeted test 명령이 성공했다.

## Follow-up Candidates

- Redis/PostgreSQL 기반 lesson issue, repository query correctness, scheduler/consumer/recovery/open-run concurrency는 integration 전용 사이클로 분리한다.
- Payment failure ledger와 성공/취소 history의 non-empty happy path는 추가 unit cycle 후보로 남긴다.
- Coupon repository naming 또는 service parameter naming은 도메인 계약 정리 이슈로 별도 논의할 수 있다.
