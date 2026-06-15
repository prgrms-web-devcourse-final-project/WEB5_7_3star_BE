# Lesson Domain Cycle Summary

## Scope

- 작업 묶음: unit-test-coverage
- 작업 주제: lesson-domain
- 범위: 레슨 강사/수강생 서비스 도메인 단위 테스트
- 제외: 통합 테스트, 운영 코드 변경, build.gradle/test profile/Testcontainers 변경

## Implemented Tests

- `AdminLessonServiceTest` 보강
  - `updateLesson`: 비모집 상태, 수정 시간 제한, 변경 필드 없음, 승인 참가자 존재 시 제한 필드 수정 예외
  - `processLessonApplication`: 거절 처리, 정원 초과 승인 예외, 이미 처리된 신청 예외, 타 강사 접근 예외
- `StudentLessonServiceTest` 신규 추가
  - `applyToApprovalLesson`: 성공, 개설자 신청 금지, 이미 참가/이미 신청 중복, 비모집 상태, open-run 경로 차단
  - `cancelLessonApplication`: 성공, open-run 취소 차단, 신청 없음, PENDING 외 상태 취소 차단
  - `getAsyncApplyStatus`: requestId 없음, WAITING 순번 포함, WAITING이지만 대기열 없음, PROCESSING, SUCCESS

## Validation

- 성공:
  - `./gradlew test --tests "com.threestar.trainus.domain.lesson.teacher.service.AdminLessonServiceTest" --tests "com.threestar.trainus.domain.lesson.student.service.StudentLessonServiceTest"`
  - 결과: `AdminLessonServiceTest` 15개, `StudentLessonServiceTest` 15개, 총 30개 테스트 통과
- 전체 테스트:
  - `./gradlew test`
  - 결과: 실패
  - 실패 원인: 기존 Spring context placeholder resolution 실패, 기존 UserCoupon/Comment/Review 컨텍스트 실패, `LessonApplyLockTest` PostgreSQL 연결 실패 등 승인 범위 밖 외부 환경/통합성 테스트 의존

## Review Result

- Reviewer 서브에이전트: `gpt-5.5`, reasoning effort `medium`
- 산출물: `review-report.md`
- 심각도 높은 문제: 없음
- 보완 권장 반영:
  - `processLessonApplication` 정원 초과 승인 예외 테스트 추가
  - `getAsyncApplyStatus` 직접 `PROCESSING` 상태 반환 테스트 추가

## Follow-up Candidates

- Controller slice/WebMvc 테스트
- mapper/DTO validation 테스트
- Redis Stream consumer/recovery, waiting room, open-run 동시성 통합 테스트
- PostgreSQL repository 검색 쿼리 통합 테스트
- 단위/통합 테스트 실행 task 분리 및 Testcontainers 적용 검토

## Broad Follow-up Implementation

### Scope

- 기존 `lesson-domain` 사이클에 추가 반영
- 승인 범위 안에서 서비스 단위 테스트, 순수 매퍼 테스트, DTO helper 테스트만 추가
- 운영 코드, `build.gradle`, test profile, controller slice, Testcontainers, 통합 테스트는 변경하지 않음

### Added / Expanded Tests

- `AdminLessonServiceTest`
  - `createLesson`: 종료 시간 검증, 최대 인원 초과, 시간 중복, open-run 이미지 저장 및 Redis stock 동기화
  - `deleteLesson`: 승인 참가자 존재, 삭제 시간 제한, 이미 삭제된 레슨
  - `getLessonApplications`: ALL 빈 목록, invalid status
  - `getLessonParticipants`: 빈 목록
  - `getCreatedLessons`: 상태 필터, invalid enum status
- `StudentLessonServiceTest`
  - `applyToOpenRunLesson`: 성공, 중복, 신청 불가
  - `getLessonDetail`: 성공, 프로필 없음
  - `getMyLessonApplications`: ALL 빈 목록, invalid status
  - `getLessonSimple`: 성공
  - `searchLessons`: invalid sort, 검색어 없음 빈 결과
  - `searchLessonsByLocation`: invalid sort, 검색어 없음 빈 결과
- 신규 순수 단위 테스트
  - `LessonMapperTest`: entity/response/detail/update response mapping
  - `LessonSimpleMapperTest`: simple response mapping
  - `LessonApplyMapperTest`: application response mapping
  - `LessonUpdateRequestDtoTest`: helper 판단 분기

### Validation

- 성공:
  - `./gradlew test --tests "com.threestar.trainus.domain.lesson.teacher.service.AdminLessonServiceTest" --tests "com.threestar.trainus.domain.lesson.student.service.StudentLessonServiceTest" --tests "com.threestar.trainus.domain.lesson.teacher.mapper.LessonMapperTest" --tests "com.threestar.trainus.domain.lesson.student.mapper.LessonSimpleMapperTest" --tests "com.threestar.trainus.domain.lesson.student.mapper.LessonApplyMapperTest" --tests "com.threestar.trainus.domain.lesson.teacher.dto.LessonUpdateRequestDtoTest"`
  - 결과: 65개 테스트 통과
- 도메인 패키지 전체 패턴:
  - `./gradlew test --tests "com.threestar.trainus.domain.lesson.*"`
  - 결과: 실패
  - 실패 원인: 기존 `LessonApplyLockTest` PostgreSQL 연결 실패, `LessonDatabaseSetupTest` placeholder resolution 실패, `LessonSearchPerformanceTest` context load 실패
  - 판단: 이번 broad follow-up 단위 테스트 변경으로 발생한 실패가 아니라 기존 외부 환경/통합성 테스트 의존

### Review Result

- Reviewer 서브에이전트 결과: blocking finding 없음
- 잔여 리스크:
  - worktree에는 이번 승인 범위 밖 기존 변경(`build.gradle`, optimizer untracked 등)이 있으므로 PR 구성 시 분리 필요
  - Redis/PostgreSQL/consumer/recovery/open-run 동시성은 여전히 통합 테스트 후보
