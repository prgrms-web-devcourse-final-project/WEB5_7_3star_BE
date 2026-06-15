# Research Report

## Core Domain Coverage Research - 2026-06-13

### 실행 정보

```text
실행 일시: 2026-06-13
사용 모델: gpt-5.4-mini
reasoning effort: high
에이전트 역할: Researcher
사용 프롬프트: core-domain unit-test coverage research
참조 문서:
- docs/ai-test-workflow/AGENTS.md
- docs/ai-test-workflow/rules/agent-roles.md
- docs/ai-test-workflow/rules/human-approval-policy.md
- docs/ai-test-workflow/rules/test-boundary.md
- docs/ai-test-workflow/rules/enforcement-checklist.md
- docs/ai-test-workflow/prompts/researcher.md
- docs/ai-test-workflow/templates/research-report-template.md
조사 범위:
- payment/refund/coupon calculation and state logic
- lesson search condition validation/limit/sort
- user authorization validation
- lesson application status/response/failure reason branches
- meaningful DTO validation and mapper logic
```

### 현재 테스트 현황

| 영역 | 현재 테스트 | 비고 |
| --- | --- | --- |
| payment/refund/coupon state | `UserCouponServiceTests`, `AdminCouponServiceTest`, `UserCouponServiceConcurrencyTests`, `UserCouponServiceRedissonLockTests` | payment 전용 unit test는 확인되지 않았다. concurrency 계열은 `@SpringBootTest`라 통합 성격이다. |
| lesson search validation/limit/sort | `StudentLessonServiceTest`의 invalid sort/no-keyword 분기, `LessonSearchPerformanceTest`, `LessonDatabaseSetupTest` | search mapper와 controller slice 테스트는 없다. performance 테스트는 커버리지 고정용이 아니다. |
| user authorization validation | `UserServiceTest`, `LoginUserArgumentResolverUnitTest` | `getAdminUser`, `validateUserExists`, `withdraw` 분기와 controller-level auth 검증은 약하다. |
| lesson application status/response/failure reason | `AdminLessonServiceTest`, `StudentLessonServiceTest`, `LessonApplyLockTest` | approve/deny/status 일부는 있으나 응답 래핑과 조회 분기 전반은 아직 넓지 않다. |
| DTO validation and mapper logic | `LessonMapperTest`, `LessonApplyMapperTest`, `LessonSimpleMapperTest`, `LessonUpdateRequestDtoTest` | `LessonSearchMapper`, `AdminCouponMapper`, `LessonApplicationMapper`, create/update/login/payment request DTO validation은 비어 있다. |

### 조사한 파일

```text
- build.gradle
- src/test/resources/application-test.yml
- src/test/java/com/threestar/trainus/domain/user/service/UserServiceTest.java
- src/test/java/com/threestar/trainus/global/resolver/LoginUserArgumentResolverUnitTest.java
- src/test/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonServiceTest.java
- src/test/java/com/threestar/trainus/domain/lesson/teacher/service/AdminLessonServiceTest.java
- src/test/java/com/threestar/trainus/domain/lesson/teacher/mapper/LessonMapperTest.java
- src/test/java/com/threestar/trainus/domain/lesson/teacher/dto/LessonUpdateRequestDtoTest.java
- src/test/java/com/threestar/trainus/domain/lesson/student/mapper/LessonApplyMapperTest.java
- src/test/java/com/threestar/trainus/domain/lesson/student/mapper/LessonSimpleMapperTest.java
- src/test/java/com/threestar/trainus/domain/metadata/service/ProfileMetadataServiceTest.java
- src/test/java/com/threestar/trainus/coupon/user/UserCouponServiceTests.java
- src/test/java/com/threestar/trainus/coupon/user/UserCouponServiceConcurrencyTests.java
- src/test/java/com/threestar/trainus/coupon/user/UserCouponServiceRedissonLockTests.java
- src/test/java/com/threestar/trainus/coupon/user/UserCouponControllerTests.java
- src/test/java/com/threestar/trainus/domain/lesson/student/LessonApplyLockTest.java
- src/main/java/com/threestar/trainus/domain/payment/service/PaymentService.java
- src/main/java/com/threestar/trainus/domain/payment/controller/PaymentController.java
- src/main/java/com/threestar/trainus/domain/payment/dto/PaymentRequestDto.java
- src/main/java/com/threestar/trainus/domain/payment/dto/ConfirmPaymentRequestDto.java
- src/main/java/com/threestar/trainus/domain/payment/dto/cancel/CancelPaymentRequestDto.java
- src/main/java/com/threestar/trainus/domain/coupon/user/service/CouponService.java
- src/main/java/com/threestar/trainus/domain/coupon/user/entity/Coupon.java
- src/main/java/com/threestar/trainus/domain/coupon/admin/service/AdminCouponService.java
- src/main/java/com/threestar/trainus/domain/coupon/admin/mapper/AdminCouponMapper.java
- src/main/java/com/threestar/trainus/domain/coupon/admin/dto/CouponCreateRequestDto.java
- src/main/java/com/threestar/trainus/domain/coupon/admin/dto/CouponUpdateRequestDto.java
- src/main/java/com/threestar/trainus/domain/lesson/student/service/StudentLessonService.java
- src/main/java/com/threestar/trainus/domain/lesson/student/controller/StudentLessonController.java
- src/main/java/com/threestar/trainus/domain/lesson/student/mapper/LessonSearchMapper.java
- src/main/java/com/threestar/trainus/domain/lesson/student/mapper/LessonApplicationMapper.java
- src/main/java/com/threestar/trainus/domain/lesson/student/dto/LessonSearchResponseDto.java
- src/main/java/com/threestar/trainus/domain/lesson/student/dto/LessonApplyStatusResponseDto.java
- src/main/java/com/threestar/trainus/domain/lesson/student/dto/MyLessonApplicationResponseDto.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/dto/LessonCreateRequestDto.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/dto/LessonUpdateRequestDto.java
- src/main/java/com/threestar/trainus/domain/lesson/teacher/dto/ApplicationActionRequestDto.java
- src/main/java/com/threestar/trainus/global/dto/PageRequestDto.java
```

### Priority Gap Analysis

#### 1. payment/refund/coupon calculation and state logic

```text
- `PaymentService` 전용 unit test가 없다. `preparePayment`, `processConfirm`, `processCancel`, `viewAllSuccessTransaction`, `viewAllFailureTransaction`, `validateDuplicatedPayment`의 분기와 예외가 현재 고정되지 않았다.
- `CouponService`는 발급/중복/만료/open-run 수량 예외는 확인되지만, `calculateDiscountedPrice`의 `%` vs `원` 분기, `useCoupon`/`restoreCoupon`의 상태 가드, `getValidUserCoupon`의 ACTIVE-only 필터는 직접 unit test가 없다.
- `Coupon.decreaseQuantity()`와 `AdminCouponService`의 수정/삭제 검증은 일부 커버되지만, 할인값 파싱 실패나 환불 후 쿠폰 복원 실패 같은 상태 전이 회귀는 아직 취약하다.
```

#### 2. lesson search condition validation/limit/sort

```text
- `StudentLessonServiceTest`는 null sort와 keyword 유무에 따른 repository 분기만 일부 확인한다.
- `page/limit` 경계, `Category.ALL` 처리, `LessonSortType` 별 정렬 property 고정은 빠져 있다.
- `LessonSearchMapper`는 순수 mapper라 unit test로 잠그기 좋지만 현재 없다.
- controller level의 `@Valid @ModelAttribute PageRequestDto`, sort enum, 좌표 파라미터 바인딩은 slice/integration 후보로 분리하는 것이 적절하다.
```

#### 3. user authorization validation

```text
- `LoginUserArgumentResolverUnitTest`는 principal null/non-long/unauthenticated를 커버한다.
- `UserServiceTest`는 `validateAdminRole` 중심이며, `getAdminUser`, `validateUserExists`, `withdraw`의 active-application/instructor-lesson 차단 분기는 약하다.
- controller-level auth validation은 이번 unit 범위보다 slice test 후보가 적절하다.
```

#### 4. lesson application status/response/failure reason branches

```text
- `AdminLessonServiceTest`는 approve/deny, capacity exceeded, already processed, access forbidden, not found, invalid status를 포함해 강한 편이다.
- `StudentLessonServiceTest`는 approval apply, cancel, open-run apply, async status(WAITING/PROCESSING/SUCCESS/NOT_FOUND)를 커버한다.
- 남은 공백은 `getMyLessonApplications`의 status parsing/ALL/empty/count boundary, search/detail/simple 응답 shape, controller status code/param binding이다.
```

#### 5. meaningful DTO validation and mapper logic

```text
- `LessonMapperTest`, `LessonApplyMapperTest`, `LessonSimpleMapperTest`, `LessonUpdateRequestDtoTest`는 존재하지만 범위가 좁다.
- `LessonCreateRequestDto`, `LessonUpdateRequestDto`, `ApplicationActionRequestDto`, `CouponCreateRequestDto`, `CouponUpdateRequestDto`, `LoginRequestDto`, `PaymentRequestDto`, `ConfirmPaymentRequestDto`, `CancelPaymentRequestDto`, `PageRequestDto`의 Bean Validation 제약은 실제 검증 테스트가 없다.
- `LessonSearchMapper`, `AdminCouponMapper`, `LessonApplicationMapper`, `CreatedLessonMapper`, `LessonParticipantMapper`, `ProfileMetadataMapper`는 응답 shape 회귀를 막는 순수 단위 테스트 후보다.
```

### High-risk Regression Candidates

| 우선순위 | 대상 | 필요한 이유 | 테스트 유형 |
| --- | --- | --- | --- |
| P1 | `PaymentService.preparePayment/processConfirm/processCancel` | 결제 준비-확정-취소 사이의 상태 전이, 쿠폰 use/restore, 환불 금액 계산이 깨지면 사용자 영향이 크다. | 단위 |
| P1 | `CouponService.calculateDiscountedPrice/getValidUserCoupon/useCoupon/restoreCoupon` | 퍼센트/정액 계산과 ACTIVE/INACTIVE 가드가 결제 금액 및 쿠폰 회수에 직결된다. | 단위 |
| P1 | `StudentLessonService.searchLessons/searchLessonsByLocation` | sort/page/filter 조합이 API 검색 결과를 곧바로 바꾼다. | 단위 |
| P1 | `LoginUserArgumentResolver` + `UserService` auth guards | 인증 principal 추출과 관리자 권한 검증은 모든 보호 API의 공통 전제다. | 단위 |
| P2 | `LessonSearchMapper` / `AdminCouponMapper` / `LessonApplicationMapper` | 응답 shape, 좌표, 이미지, count 래핑 회귀를 낮은 비용으로 잡을 수 있다. | 단위 |
| P3 | `LessonApplyConsumer` / recovery / stock reconciliation | Redis Stream과 recovery는 외부 상태 의존이 커서 통합에서 더 안정적으로 잡힌다. | 통합 |

### Unit-Test Candidates vs Integration-Only Candidates

```text
Unit-test candidates:
- `PaymentService`의 계산/상태 분기
- `CouponService`의 discount/use/restore/validation 분기
- `StudentLessonService`의 search/apply/cancel/status 문자열 분기
- `UserService`의 auth/role guard
- `LoginUserArgumentResolver`
- mapper와 DTO helper/validation

Integration-only candidates:
- `UserCouponServiceConcurrencyTests`, `UserCouponServiceRedissonLockTests`
- `LessonApplyLockTest`
- `LessonSearchPerformanceTest`, `LessonDatabaseSetupTest`
- Redis Stream consumer, pending recovery, stock reconciliation
- repository query correctness for PostgreSQL/PostGIS search paths
```

### Testcontainers / External Dependency Notes

```text
필요 여부: 높음
적용 후보: PostgreSQL/PostGIS search repository, Redis Stream consumer, open-run concurrent apply, recovery/stock reconciliation, payment history persistence checks
적용하지 않아도 되는 영역: pure service branch tests, mapper, DTO validation, resolver
이유: `application-test.yml`이 PostgreSQL/Redis를 직접 바라보고 있고, build.gradle에는 `integrationTest` task 분리가 없다. H2는 들어 있으나 현재 검색/stream 핵심 경로의 실제 의존성은 외부 인프라에 있다.
```

### Recommended Cycle Name

```text
권장 주제명: core-domain-branch-coverage
이유: 이번 조사 범위는 lesson-domain을 넘어 payment/refund/coupon, auth, search, DTO/mapper를 함께 묶는 cross-cutting branch coverage 성격이 강하다.
```

### Researcher Conclusion

```text
요약:
- lesson-domain에는 일부 서비스 단위 테스트와 Redis/PostgreSQL 기반 통합/벤치마크 테스트가 섞여 있지만, payment/refund/coupon state, search validation, auth, mapper/DTO validation은 아직 빈 구간이 많다.
- 단위 테스트로 가장 효율적으로 잠글 수 있는 영역은 `PaymentService`, `CouponService`, `StudentLessonService`의 분기 로직, `UserService` 권한 검증, 그리고 순수 mapper/DTO helper다.
- Redis Stream consumer, recovery scheduler, open-run 동시성, PostgreSQL search query는 통합 테스트로 분리하는 편이 맞다.

다음 단계에서 Planner가 판단해야 할 사항:
- P1으로 묶을 payment/coupon/search/auth 단위 테스트 우선순위
- controller slice를 이번 사이클에 포함할지 여부
- Redis/PostgreSQL 의존 경로를 Testcontainers로 넘길지 여부
- DTO validation을 unit 범주에 얼마나 넓게 넣을지
```
