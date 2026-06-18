# PR Draft - Lesson Domain Unit Test Coverage

## Summary

- Broaden lesson-domain-unit-coverage unit test coverage for teacher and student lesson services.
- Add pure mapper and DTO helper tests for approved lesson-domain-unit-coverage boundaries.
- Keep changes test-only; no production code, build, profile, controller slice, Testcontainers, or integration test changes.

## Changes

- Expanded `AdminLessonServiceTest`
  - creation validation: invalid end time, max participants exceeded, time conflict
  - open-run creation side effects: image save and Redis stock sync
  - deletion validation: approved participants, time limit, already deleted lesson
  - query/list boundaries: empty applications, invalid application status, empty participants, created lessons status filter
- Expanded `StudentLessonServiceTest`
  - open-run async request mapping and queue failure cases
  - detail/simple lesson lookup
  - my application list status parsing
  - search and location-search invalid sort and empty-result paths
- Added mapper/DTO helper tests
  - `LessonMapperTest`
  - `LessonSimpleMapperTest`
  - `LessonApplyMapperTest`
  - `LessonUpdateRequestDtoTest`

## Validation

```bash
./gradlew test \
  --tests "com.threestar.trainus.domain.lesson.teacher.service.AdminLessonServiceTest" \
  --tests "com.threestar.trainus.domain.lesson.student.service.StudentLessonServiceTest" \
  --tests "com.threestar.trainus.domain.lesson.teacher.mapper.LessonMapperTest" \
  --tests "com.threestar.trainus.domain.lesson.student.mapper.LessonSimpleMapperTest" \
  --tests "com.threestar.trainus.domain.lesson.student.mapper.LessonApplyMapperTest" \
  --tests "com.threestar.trainus.domain.lesson.teacher.dto.LessonUpdateRequestDtoTest"
```

- Result: passed, 65 tests.

```bash
./gradlew test --tests "com.threestar.trainus.domain.lesson.*"
```

- Result: failed due existing environment-dependent/context tests:
  - `LessonApplyLockTest`: PostgreSQL connection failure
  - `LessonDatabaseSetupTest`: placeholder resolution failure
  - `LessonSearchPerformanceTest`: context load failure

## Review

- Reviewer finding: no blocking issue in the six requested test files.
- Scope note: current worktree includes unrelated existing changes outside this cycle; include only approved lesson-domain-unit-coverage test/report files in the PR.
