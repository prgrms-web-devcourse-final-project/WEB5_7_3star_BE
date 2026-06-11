# Research Report

## Status
- Completed

## Scope
- `src/test` 전체
- `build.gradle` 테스트 설정
- `src/test/resources` 테스트 profile
- Redis/PostgreSQL 의존 테스트 여부
- Lesson 신청, Redis Stream, Waiting Room, 재고 보정 스케줄러
- 지역/위치 검색
- 인증 사용자 조회 관련 테스트 현황

## 1. 현재 존재하는 테스트 유형
- Mockito 기반 단위 테스트
  - `UserServiceTest`
  - `AdminLessonServiceTest`
  - `AdminCouponServiceTest`
  - `ProfileMetadataServiceTest`
  - `LessonCreationLimitServiceTest`
  - `CouponStatusSchedulerTest`
  - `LessonStatusSchedulerTest`
  - `UserCouponServiceTests`
- SpringBoot 기반 통합 테스트
  - `CommentControllerTest`
  - `ReviewControllerTest`
- Redis/DB 동시성 통합 테스트
  - `LessonApplyLockTest`
  - `UserCouponServiceConcurrencyTests`
  - `UserCouponServiceRedissonLockTests`
- 성능/데이터 생성성 테스트
  - `LessonSearchPerformanceTest`
  - `LessonDatabaseSetupTest`
- 엔티티 단위 테스트
  - `ProfileTest`
  - `UserTest`
- 스모크 테스트
  - `TrainUsApplicationTests`
- 주석 처리된 테스트 초안
  - `UserCouponControllerTests`
  - `KeywordSearchPerformanceTest`
  - `LocationSearchPerformanceTest`

## 2. 단위 테스트 / 통합 테스트 현황
- 단위 테스트는 서비스 계층 중심으로 비교적 잘 분리되어 있다.
- 대부분 Mockito 기반이라 외부 인프라 없이 도메인 분기와 예외 처리를 검증한다.
- 통합 테스트는 존재하지만 범위가 좁다.
- `@DataJpaTest`와 `@WebMvcTest`는 활성 테스트에서 확인되지 않았다.
- `@SpringBootTest`는 컨트롤러, 동시성, 검색 성능, 데이터 생성 테스트에 집중되어 있다.
- `application-test.yml`은 PostgreSQL/Redis 환경변수를 필요로 하므로, 테스트 실행이 외부 환경에 의존한다.
- 추정: 현재 통합 테스트는 Testcontainers 없이 실제 Redis/PostgreSQL 또는 그에 준하는 환경을 기대한다.

## 3. 테스트가 부족한 도메인
- 인증 사용자 조회 경로
  - `LoginUserArgumentResolver`
  - `UserController.getCurrentUser`
  - `UserController.withdraw`
- 프로필 컨트롤러 경로
  - `ProfileController.updateProfileImage`
  - `ProfileController.updateProfileIntro`
  - `ProfileController.getProfileDetail`
  - `ProfileController.getUserCreatedLessons`
- 관리자 컨트롤러 경로
  - `AdminLessonController`
  - `AdminCouponController`
- Redis Stream / Waiting Room / Admission / Reconciliation 경로
  - `LessonApplyProducer`
  - `LessonWaitingRoomService`
  - `LessonAdmissionScheduler`
  - `LessonStockReconciliationScheduler`
- 지역/위치 검색
  - 현재는 성능 로그 중심이고, 결과 정확성을 고정하는 회귀 테스트가 부족하다.

## 4. 회귀 테스트가 필요한 고위험 경로
- 중복 신청이 DB에 한 번만 반영되는지
- Consumer 시스템 실패 시 ACK / XDEL 하지 않는지
- Admission 실패 시 requestId가 유실되지 않는지
- 보정 스케줄러가 처리 중 상태를 오판하지 않는지
- 지역/위치 검색이 기존 필터와 함께 정상 동작하는지
- 인증 사용자 조회가 SecurityContext와 `@LoginUser` 경로를 정확히 해석하는지

## 5. Testcontainers 적용 필요 여부
- 필요하다.
- 이유:
  - 활성 테스트 중 Redis/PostgreSQL 의존 경로가 있다.
  - `src/test/resources/application-test.yml`이 `DB_HOST`, `DB_PORT`, `DB_NAME`, `REDIS_HOST`, `DDL_AUTO`를 외부에서 주입받는다.
  - `build.gradle`에 H2는 있지만, 현재 활성 테스트 구조는 H2 격리형으로 보이지 않는다.
  - Redis Stream, waiting room, PostgreSQL 검색 쿼리, transaction 경계는 Testcontainers로 검증하는 편이 재현성이 높다.
- 단위 테스트에는 불필요하다.
- 통합 테스트와 회귀 테스트에 우선 적용하는 것이 적절하다.

## 6. 우선순위 높은 테스트 후보 5개
1. `LoginUserArgumentResolver.resolveArgument`
   - null authentication / unauthenticated / principal 타입 불일치 / 정상 principal 처리
2. `UserController.getCurrentUser`
   - `@LoginUser` 해석 포함 현재 사용자 조회 흐름
3. `LessonAdmissionScheduler` + `LessonWaitingRoomService`
   - dequeue, pipeline, requeue 실패 복원, 빈 큐 처리
4. `LessonStockReconciliationScheduler.reconcileStock`
   - waiting-room 존재, stream lag, busy-state, dirty-set 제거 확인
5. `LessonRepository` 지역/위치/키워드 검색 회귀 테스트
   - 필터 조합과 페이지 결과 정확성 검증

## 7. 조사한 파일 목록
- `docs/ai-test-workflow/rules/agent-roles.md`
- `docs/ai-test-workflow/rules/human-approval-policy.md`
- `docs/ai-test-workflow/rules/test-boundary.md`
- `build.gradle`
- `src/test/resources/application-test.yml`
- `src/test/java/com/threestar/trainus/TrainUsApplicationTests.java`
- `src/test/java/com/threestar/trainus/coupon/user/UserCouponControllerTests.java`
- `src/test/java/com/threestar/trainus/coupon/user/UserCouponServiceConcurrencyTests.java`
- `src/test/java/com/threestar/trainus/coupon/user/UserCouponServiceRedissonLockTests.java`
- `src/test/java/com/threestar/trainus/coupon/user/UserCouponServiceTests.java`
- `src/test/java/com/threestar/trainus/domain/comment/controller/CommentControllerTest.java`
- `src/test/java/com/threestar/trainus/domain/coupon/admin/scheduler/CouponStatusSchedulerTest.java`
- `src/test/java/com/threestar/trainus/domain/coupon/admin/service/AdminCouponServiceTest.java`
- `src/test/java/com/threestar/trainus/domain/lesson/student/KeywordSearchPerformanceTest.java`
- `src/test/java/com/threestar/trainus/domain/lesson/student/LessonApplyLockTest.java`
- `src/test/java/com/threestar/trainus/domain/lesson/student/LessonDatabaseSetupTest.java`
- `src/test/java/com/threestar/trainus/domain/lesson/student/LessonSearchPerformanceTest.java`
- `src/test/java/com/threestar/trainus/domain/lesson/student/LocationSearchPerformanceTest.java`
- `src/test/java/com/threestar/trainus/domain/lesson/teacher/scheduler/LessonStatusSchedulerTest.java`
- `src/test/java/com/threestar/trainus/domain/lesson/teacher/service/AdminLessonServiceTest.java`
- `src/test/java/com/threestar/trainus/domain/lesson/teacher/service/LessonCreationLimitServiceTest.java`
- `src/test/java/com/threestar/trainus/domain/metadata/service/ProfileMetadataServiceTest.java`
- `src/test/java/com/threestar/trainus/domain/profile/entity/ProfileTest.java`
- `src/test/java/com/threestar/trainus/domain/review/controller/ReviewControllerTest.java`
- `src/test/java/com/threestar/trainus/domain/user/entity/UserTest.java`
- `src/test/java/com/threestar/trainus/domain/user/service/UserServiceTest.java`

## 8. 사용한 CodeGraph / 명령 목록
- CodeGraph
  - `codegraph_context`
  - `codegraph_files`
  - `codegraph_search`
  - `codegraph_node`
  - `codegraph_explore`
- 주요 조회 대상
  - `UserController`
  - `ProfileController`
  - `AdminLessonController`
  - `AdminCouponController`
  - `CommentController`
  - `ReviewController`
  - `LoginUserArgumentResolver`
  - `resolveArgument`
  - `getCurrentUserInfo`
  - `LessonStockReconciliationScheduler.reconcileStock`
  - `LessonAdmissionScheduler.admitUsers`
  - `LessonAdmissionScheduler.processAdmissionForLesson`
  - `LessonWaitingRoomService`
  - `LessonApplyProducer`
- 쉘 명령
  - `sed -n` 로 문서/설정/테스트 파일 확인
  - `rg -n` 으로 annotation, test, auth, Testcontainers, Redis, PostgreSQL 검색
  - `rg --files` 로 `src/test/java`, `src/test/resources` 구조 확인
