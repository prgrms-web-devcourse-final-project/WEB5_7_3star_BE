# Review Report

## 심각도 높은 문제

| 항목 | 위치 | 문제 | 권장 조치 |
| --- | --- | --- | --- |
| H1 | `src/test/java/com/threestar/trainus/domain/lesson/student/LessonApplyLockTest.java:274` | Implementer가 `LessonApplyLockTest`의 컴파일 오류는 해소했지만, 이 테스트는 `@SpringBootTest` + `local`, `consumer` profile + Redis/PostgreSQL 의존 + 10,000명 동시 요청 성격이라 이번 Reviewer 검증에서는 실행 성공까지 확인되지 않았다. `compileTestJava` 통과와 해당 통합/성능 테스트 통과는 다른 주장이다. | 포트폴리오나 산출물에는 `LessonApplyLockTest`는 "컴파일 오류 해소"까지만 주장하고, Redis/PostgreSQL 실행 환경에서의 통합 테스트 성공은 별도 검증 후 주장해야 한다. |

## 보완하면 좋은 문제

| 항목 | 위치 | 문제 | 권장 조치 |
| --- | --- | --- | --- |
| M1 | `src/test/java/com/threestar/trainus/domain/lesson/student/LessonApplyLockTest.java:295` | `LessonApplyProducer.send()`는 정상 requestId 외에도 중복 신청 시 `"ALREADY_APPLIED"`를 반환한다. 현재 테스트는 `requestId != null`이면 producer 성공으로 집계하므로, 향후 중복 요청 시나리오가 섞이면 duplicate reject를 성공으로 오인할 수 있다. 현재 10,000명 고유 사용자 setup에서는 직접 실패 요인은 아니다. | MQ 테스트의 성공 기준을 "대기열 등록 requestId"와 "중복 거절 sentinel"로 분리하면 결과 로그의 의미가 더 명확해진다. |
| M2 | `src/test/java/com/threestar/trainus/global/resolver/LoginUserArgumentResolverUnitTest.java:57` | `resolveArgument` 테스트는 `parameter`, `mavContainer`, `webRequest`, `binderFactory`를 모두 `null`로 전달한다. 현재 운영 코드가 해당 인자를 사용하지 않아 resolver 내부 분기 검증에는 충분하지만, Spring MVC argument resolving wiring 검증으로 볼 수는 없다. | 포트폴리오에는 "resolver 내부 인증 분기 단위 검증"으로 표현하고, 실제 컨트롤러 파라미터 주입은 별도 controller integration test에서 다뤄야 한다. |

## 테스트 신뢰성 리스크

| 항목 | 위치 | 리스크 | 판단 |
| --- | --- | --- | --- |
| R1 | `LoginUserArgumentResolverUnitTest.java:26` | `SecurityContextHolder`는 전역 ThreadLocal 상태를 사용한다. | `@AfterEach`에서 `SecurityContextHolder.clearContext()`를 호출하고, targeted test 재실행이 성공했으므로 단위 테스트 cleanup 구조는 적절하다. |
| R2 | `LoginUserArgumentResolverUnitTest.java:21` | resolver 테스트가 Spring context 없이 직접 인스턴스와 Mockito `Authentication`을 사용한다. | 단위 테스트 경계는 지켜졌다. 외부 인프라나 Spring context 의존으로 인한 flaky 리스크는 낮다. |
| R3 | `UserServiceTest.java:140` | 로그인 테스트가 현재 `UserService.login(LoginRequestDto)`와 JWT 반환 계약에 맞춰 수정되었다. | `JwtProvider` mock으로 access/refresh token을 고정해 실제 동작을 검증한다. targeted 재실행 성공으로 현재 계약과 일치한다. |
| R4 | `LessonApplyLockTest.java:32` | `LessonApplyLockTest`는 Spring context, local/consumer profile, Redis, DB, 대량 데이터 setup, thread sleep/wait를 포함한다. | 단위 테스트 범위가 아니며 실행 환경에 민감하다. 이번 리뷰에서 실행하지 않았으므로 성공 주장 대상에서 제외해야 한다. |

## 검토 기준별 판단

| 기준 | 판단 |
| --- | --- |
| `supportsParameter()` 실제 동작 검증 | `@LoginUser Long` true, annotation 없는 Long false, `@LoginUser String` false, `@LoginUser long` false를 reflection 기반 `MethodParameter`로 검증하므로 충분하다. |
| `resolveArgument()` 실제 동작 검증 | null authentication, unauthenticated, non-Long principal, null principal, Long principal 반환을 직접 검증하므로 핵심 예외/정상 분기를 커버한다. |
| 예외/정상 분기 커버 | resolver 기준 충분하다. `UserServiceTest`의 로그인 성공/실패와 관리자 권한 실패 기대값도 현재 운영 코드 계약에 맞다. |
| 테스트 이름 명확성 | resolver 테스트의 DisplayName과 메서드명은 조건과 기대 결과를 드러낸다. |
| Mockito / AssertJ / JUnit 5 일관성 | resolver와 UserService 단위 테스트는 JUnit 5, AssertJ, Mockito 조합이 일관적이다. |
| SecurityContext 정리 | resolver 테스트가 `@AfterEach`에서 `SecurityContextHolder.clearContext()`를 호출한다. |
| 단위 테스트 범위 | resolver 테스트는 단위 테스트 범위를 지킨다. `LessonApplyLockTest`는 통합/성능 성격이므로 별도 검증 범위다. |
| 포트폴리오 주장 수준 | resolver 단위 테스트 작성과 targeted test 통과는 주장 가능하다. 전체 테스트 통과, LessonApplyLockTest 실행 성공, Spring MVC wiring 검증은 아직 주장하면 안 된다. |

## 포트폴리오에 사용할 수 있는 강점

```text
- HandlerMethodArgumentResolver의 지원 조건을 `@LoginUser`와 `Long` 타입 조합으로 분리해 검증했다.
- SecurityContext 기반 인증 실패, 인증되지 않은 사용자, null principal, principal 타입 불일치, 정상 Long principal 반환을 단위 테스트로 고정했다.
- Spring context를 띄우지 않고 reflection `MethodParameter`와 Mockito `Authentication`으로 단위 테스트 경계를 유지했다.
- `@AfterEach`에서 SecurityContextHolder를 정리해 전역 인증 상태 오염을 방지했다.
- 기존 테스트 컴파일 차단 원인을 현재 운영 코드 계약에 맞게 정리해 resolver/UserService targeted test를 실제 재실행했다.
```

## 아직 주장하면 안 되는 내용

```text
- 전체 `./gradlew test`가 통과했다는 주장
- `LessonApplyLockTest`의 Redis/PostgreSQL 의존 통합/성능 시나리오가 실행 성공했다는 주장
- Spring MVC에서 `@LoginUser`가 컨트롤러 파라미터로 실제 주입되는 wiring까지 검증했다는 주장
- 인증 사용자 조회 경로의 controller integration test가 완료되었다는 주장
- coverage 수치가 개선되었다는 주장
```

## 실행 정보

```text
실행 일시: 2026-06-11 18:37:24 KST
사용 모델: GPT-5 Codex
에이전트 역할: Reviewer
사용 프롬프트: Implementer가 작업을 완료했으므로 기존과 같이 확인하고 리뷰를 진행한다.
참조 문서:
- docs/ai-test-workflow/rules/agent-roles.md
- docs/ai-test-workflow/rules/human-approval-policy.md
- docs/ai-test-workflow/rules/test-boundary.md
- docs/ai-test-workflow/reports/test-plan.md
- docs/ai-test-workflow/reports/research-report.md
검토 대상:
- src/test/java/com/threestar/trainus/global/resolver/LoginUserArgumentResolverUnitTest.java
- src/main/java/com/threestar/trainus/global/resolver/LoginUserArgumentResolver.java
- src/test/java/com/threestar/trainus/domain/user/service/UserServiceTest.java
- src/test/java/com/threestar/trainus/domain/lesson/student/LessonApplyLockTest.java
```

## 검토한 파일 목록

```text
- docs/ai-test-workflow/rules/agent-roles.md
- docs/ai-test-workflow/rules/human-approval-policy.md
- docs/ai-test-workflow/rules/test-boundary.md
- docs/ai-test-workflow/reports/test-plan.md
- docs/ai-test-workflow/reports/research-report.md
- docs/ai-test-workflow/reports/review-report.md
- docs/ai-test-workflow/logs/prompt-and-tool-log.md
- src/test/java/com/threestar/trainus/global/resolver/LoginUserArgumentResolverUnitTest.java
- src/main/java/com/threestar/trainus/global/resolver/LoginUserArgumentResolver.java
- src/test/java/com/threestar/trainus/domain/user/service/UserServiceTest.java
- src/main/java/com/threestar/trainus/domain/user/service/UserService.java
- src/main/java/com/threestar/trainus/domain/user/dto/LoginResponseDto.java
- src/main/java/com/threestar/trainus/domain/user/mapper/UserMapper.java
- src/test/java/com/threestar/trainus/domain/lesson/student/LessonApplyLockTest.java
- src/main/java/com/threestar/trainus/domain/lesson/issue/LessonApplyProducer.java
```

## 사용한 명령과 결과

| 명령 | 결과 |
| --- | --- |
| `sed -n '1,240p' docs/ai-test-workflow/rules/agent-roles.md` | Reviewer 역할, 금지 사항, 산출물 기준 확인 |
| `sed -n '1,240p' docs/ai-test-workflow/rules/human-approval-policy.md` | 리뷰 문서/로그 작성 가능 범위와 운영 코드 수정 금지 확인 |
| `sed -n '1,240p' docs/ai-test-workflow/rules/test-boundary.md` | 단위/통합 테스트 경계 확인 |
| `sed -n '1,260p' docs/ai-test-workflow/reports/test-plan.md` | resolver 단위 테스트 계획과 통합 테스트 분리 기준 확인 |
| `sed -n '1,260p' docs/ai-test-workflow/reports/research-report.md` | 인증 사용자 조회 경로와 Redis/DB 의존 테스트 리스크 확인 |
| `sed -n '1,260p' src/test/java/com/threestar/trainus/global/resolver/LoginUserArgumentResolverUnitTest.java` | null principal 케이스 추가 포함 resolver 테스트 확인 |
| `sed -n '1,220p' src/main/java/com/threestar/trainus/global/resolver/LoginUserArgumentResolver.java` | resolver 운영 코드 분기 확인 |
| `tail -n 220 docs/ai-test-workflow/logs/prompt-and-tool-log.md` | Implementer 후속 작업 로그 확인 |
| `sed -n '1,260p' docs/ai-test-workflow/reports/review-report.md` | 기존 리뷰 리포트가 Implementer 후속 조치 내용과 섞여 있음을 확인 |
| `git status --short ...` | 변경 대상과 미추적 산출물 상태 확인 |
| `nl -ba src/test/java/com/threestar/trainus/global/resolver/LoginUserArgumentResolverUnitTest.java` | 리뷰 근거용 라인 번호 확인 |
| `nl -ba src/test/java/com/threestar/trainus/domain/user/service/UserServiceTest.java` | 로그인/JWT, 관리자 권한 테스트 수정 위치 확인 |
| `nl -ba src/test/java/com/threestar/trainus/domain/lesson/student/LessonApplyLockTest.java` | MQ producer 반환값 처리 수정 위치 확인 |
| `sed -n '55,105p' src/main/java/com/threestar/trainus/domain/user/service/UserService.java` | `login(LoginRequestDto)`와 JWT 발급 계약 확인 |
| `sed -n '95,130p' src/main/java/com/threestar/trainus/domain/user/service/UserService.java` | `validateAdminRole`의 `ACCESS_FORBIDDEN` 계약 확인 |
| `sed -n '20,80p' src/main/java/com/threestar/trainus/domain/lesson/issue/LessonApplyProducer.java` | `send()`가 requestId, `"ALREADY_APPLIED"`, null을 반환하는 계약 확인 |
| `./gradlew test --tests com.threestar.trainus.global.resolver.LoginUserArgumentResolverUnitTest --tests com.threestar.trainus.domain.user.service.UserServiceTest` | Gradle up-to-date로 종료. 실제 실행 증거로는 부족 |
| `./gradlew test --rerun-tasks --tests com.threestar.trainus.global.resolver.LoginUserArgumentResolverUnitTest --tests com.threestar.trainus.domain.user.service.UserServiceTest` | 성공. `compileTestJava`와 resolver/UserService targeted test가 실제 실행됨 |
| `git diff -- src/test/java/com/threestar/trainus/domain/user/service/UserServiceTest.java` | HttpSession 기반 login 기대값이 JWT 반환 계약으로 수정된 diff 확인 |
| `git diff -- src/test/java/com/threestar/trainus/domain/lesson/student/LessonApplyLockTest.java` | `boolean sent`에서 `String requestId != null` 판정으로 수정된 diff 확인 |
| `git diff --name-only` | 이번 리뷰 외 기존 README/build.gradle/architecture 변경이 함께 존재함을 확인. Reviewer는 지정 문서/로그 외 코드 수정 없음 |

## Reviewer 결론

```text
승인 가능 여부:
- 조건부 승인 가능.
- `LoginUserArgumentResolverUnitTest`와 `UserServiceTest`는 현재 운영 코드 계약에 맞고, `--rerun-tasks` targeted test가 성공했다.
- `LessonApplyLockTest`는 컴파일 오류 해소는 확인됐지만 실행 검증은 별도 Redis/PostgreSQL 환경에서 다시 해야 한다.

남은 리스크:
- 전체 테스트 통과는 아직 확인되지 않았다.
- `LessonApplyLockTest`의 MQ producer 성공 집계는 중복 sentinel을 성공으로 볼 수 있는 표현상 리스크가 있다.
- `@LoginUser`의 실제 controller wiring 검증은 이 단위 테스트가 아니라 별도 controller integration test에서 다뤄야 한다.
```
