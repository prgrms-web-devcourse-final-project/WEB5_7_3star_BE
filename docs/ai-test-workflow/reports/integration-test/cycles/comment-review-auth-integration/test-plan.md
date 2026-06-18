# P3 댓글/리뷰 HTTP 인증 흐름 통합 테스트 계획서

## 1. 목적
이 문서는 이슈 #238의 P3 계획을 수정해서, 댓글/리뷰 HTTP 인증 흐름의 기준을 `MockHttpSession` 기반 세션 주입이 아니라 JWT 기반 인증 경계로 다시 잡기 위한 승인 전 계획을 정리한 것이다.

근거는 기존 Researcher 산출물과 현재 확인된 테스트 구조에 한정한다. 추가 리서칭, 운영 코드 수정, 테스트 코드 생성, 검증 실행은 아직 하지 않는다.

## 2. 계획 대상
이번 계획에서 다룰 후보는 다음과 같다.
- 댓글 생성 HTTP 인증 흐름
- 댓글 삭제 HTTP 인증 흐름
- 리뷰 생성 HTTP 인증 흐름
- JWT 발급 또는 테스트용 토큰 생성과 `Authorization` 헤더 주입 방식
- `SecurityFilterChain` 통과 여부와 `SecurityContextHolder` 반영 여부

핵심은 `@LoginUser` 기반 요청이 실제 HTTP 인증 흐름, 즉 `Authorization` 헤더 -> `SecurityConfig` -> `JwtAuthenticationFilter` -> `SecurityContextHolder` -> `LoginUserArgumentResolver` 경로를 실제로 통과하는지 확인하는 통합 테스트 후보를 정리하는 것이다.

## 3. 현재 관찰된 구조
### 3.1 세션 주입 테스트의 의미와 한계
- `CommentControllerTest`, `ReviewControllerTest`는 `MockHttpSession`에 `LOGIN_USER`를 넣는 방식이다.
- 이 방식은 컨트롤러 호출 시 필요한 사용자 정보를 편의상 주입하는 성격이 강하다.
- 현재 운영 인증 구조에서 실제로 통과해야 하는 JWT 필터 체인을 검증하지 못한다.
- 따라서 세션 주입 테스트는 컨트롤러 입력 바인딩 또는 과거 방식의 편의성 검증으로는 의미가 있지만, 현재 인증 흐름 검증으로는 부족하다.

### 3.2 JWT 기반 인증 흐름
- 실제 인증 흐름은 `SecurityConfig -> JwtAuthenticationFilter -> JwtProvider.getAuthentication()` 경로다.
- `JwtAuthenticationFilter`가 토큰을 해석해 `SecurityContextHolder`에 인증 정보를 넣고, `LoginUserArgumentResolver`는 그 `SecurityContextHolder`를 읽는다.
- 따라서 JWT 기반 통합 테스트는 요청 헤더부터 필터 체인, 보안 컨텍스트, 컨트롤러 인자 해석까지 이어지는 실제 경로를 검증해야 한다.

### 3.3 단위 테스트와의 경계
- `LoginUserArgumentResolverUnitTest`는 리졸버의 분기와 예외를 확인하는 단위 테스트 경계로 둔다.
- `JwtAuthenticationFilterIntegrationTest`는 보안 필터 체인과 JWT 인증 연결을 보는 통합 테스트 경계로 둔다.
- 이번 P3 계획은 이 두 테스트를 대체하지 않고, 댓글/리뷰 API의 HTTP 인증 흐름이 실제 보안 경계와 맞는지 확인하는 통합 후보를 정리하는 데 초점을 둔다.

## 4. 승인 가능한 구현 방향
승인 이후 구현은 다음 원칙으로만 진행한다.
- 댓글/리뷰 API의 HTTP 요청이 JWT 기반 인증 흐름을 실제로 타는지 확인한다.
- 세션 키 주입이 아니라 `Authorization` 헤더 기반 인증 경로와의 정합성을 기준으로 삼는다.
- 기존 세션 주입 테스트는 현재 인증 흐름 검증으로는 부족한 테스트로 분리해 설명하고, 필요 시 레거시/편의성 테스트로만 유지한다.
- 통합 테스트는 `SecurityFilterChain` 통과 여부와 `@LoginUser` 해석 결과를 함께 본다.

## 5. JWT 기반 통합 테스트 후보
### 5.1 댓글 생성
- 유효한 JWT가 있을 때 댓글 생성 요청이 성공해야 한다.
- `Authorization: Bearer <token>` 헤더를 주입한 요청이 필터 체인을 통과하고 `@LoginUser`가 실제 사용자 ID를 받는지 확인한다.
- 토큰이 없거나 형식이 잘못되면 인증 실패로 처리되어야 한다.

### 5.2 댓글 삭제
- 유효한 JWT가 있을 때 댓글 삭제 요청이 성공해야 한다.
- 삭제 권한이 소유자 기준인지, 관리자 기준인지에 따라 `SecurityContextHolder`의 인증 정보가 실제로 반영되는지 확인한다.
- 토큰 누락, 위조 토큰, 권한 부족은 각각 인증 실패 또는 인가 실패로 분리해 검증한다.

### 5.3 리뷰 생성
- 유효한 JWT가 있을 때 리뷰 생성 요청이 성공해야 한다.
- 세션 주입이 아니라 실제 JWT 인증 경로를 통해 `@LoginUser`가 채워지는지 확인한다.
- 인증 헤더가 없거나 잘못되면 요청은 차단되어야 한다.

### 5.4 인증 실패 공통 기준
- 세션이 아니라 JWT가 인증의 기준이어야 한다.
- 요청이 필터 체인에서 차단되는지, 혹은 인가 단계에서 거부되는지 상태 코드 기준으로 분리해 본다.
- 응답 성공 여부만 보지 말고, 실제로 `SecurityContextHolder` 기반 사용자 해석이 일어나는지를 함께 본다.

## 6. JWT 토큰 생성과 헤더 주입 방식
승인 후 구현에서는 다음 방식 중 하나를 택한다.
- 테스트 컨텍스트의 `JwtProvider`를 이용해 실제 서명 규칙에 맞는 토큰을 생성한다.
- 공통 테스트 support class에서 사용자 fixture와 토큰 생성 헬퍼를 제공한다.
- `MockMvc` 요청에 `Authorization` 헤더를 명시적으로 주입한다.

이 계획의 기준은 `MockHttpSession`에 `LOGIN_USER`를 넣는 방식이 아니라, 실제 인증 경로와 같은 입력 형태를 쓰는 것이다.

## 7. 기존 세션 주입 테스트를 삭제할지 유지할지 판단 기준
권장 기준은 다음과 같다.
- JWT 기반 통합 테스트가 같은 엔드포인트의 인증 성공/실패를 충분히 덮는다면, 세션 주입 테스트는 현재 인증 흐름 검증 문맥에서는 삭제 후보가 된다.
- 세션 주입 테스트가 아직 컨트롤러 입력 바인딩이나 과거 호환성 확인에 별도 가치를 가진다면, 유지하되 "현재 인증 흐름 검증용 테스트가 아니다"라고 분리해 설명한다.
- 따라서 이번 계획의 기본 방향은 "JWT 기반 통합 테스트를 정식 기준으로 추가"이고, 세션 주입 테스트는 보조적 의미만 남길 수 있는지 후속 판단한다.

## 8. 필요한 인프라와 test profile 요구사항
Researcher 산출물 기준으로 필요한 전제는 다음이다.
- `PostgresIntegrationTestSupport`를 사용하는 통합 테스트 환경
- JWT 테스트용 `jwt.secret` 주입
- Testcontainers PostgreSQL 기반 데이터 격리
- `SecurityConfig`가 전역 필터 체인을 그대로 타도록 하는 Spring Boot 통합 테스트 설정
- 필요 시 공통 JWT 토큰 생성 support를 둘 수 있는 테스트 support 패키지

추가로 필요한 프로파일은 이미 존재하는 통합 테스트 패턴을 따르는 방향으로 한정한다. 새 프로파일을 임의로 늘리지 않는다.

## 9. 데이터 setup / cleanup 전략
승인 후 구현 시 데이터 전략은 다음 범위로 제한한다.
- 테스트마다 독립된 사용자 데이터를 준비한다.
- JWT 토큰 생성에 필요한 테스트 유저는 토큰의 주체와 실제 저장된 사용자 데이터가 일치하도록 만든다.
- 댓글/리뷰 대상이 되는 게시글 또는 관련 엔티티는 각 테스트 케이스 내부에서 명시적으로 만든다.
- 종료 후에는 트랜잭션 롤백 또는 테스트 DB 초기화 방식 중 기존 프로젝트 관례를 따른다.

cleanup은 통합 테스트 간 상태 공유를 막는 수준이면 충분하고, 운영 데이터 정리나 광범위한 리셋은 하지 않는다.

## 10. 제외 범위
이번 계획에서 제외한다.
- 결제 HTTP 인증 흐름
- S3 HTTP 인증 흐름
- 운영 코드 리팩터링
- 인증 구조 자체의 변경
- 단위 테스트 추가 또는 확장
- 테스트 코드의 대규모 재작성
- 성능 테스트

결제와 S3는 동일한 `@LoginUser` 계열이라도 외부 연동과 상태 관리 성격이 달라 이번 묶음에서 분리한다.

## 11. 후속으로 분리할 후보
이번 계획 후 별도 후보로 남긴다.
- 결제 HTTP 인증 흐름
- S3 업로드/다운로드 HTTP 인증 흐름
- `@LoginUser`와 JWT 인증 연결을 더 넓게 검증하는 보안 회귀 테스트
- 세션 주입 테스트 정리 또는 삭제 판단

## 12. 수정 허용 파일
이번 계획 수정 단계에서 실제로 수정 허용되는 파일은 다음 두 파일이다.
- `docs/ai-test-workflow/reports/integration-test/cycles/comment-review-auth-integration/test-plan.md`
- `docs/ai-test-workflow/logs/integration-test/cycles/comment-review-auth-integration/prompt-and-tool-log.md`

## 13. 구현 시 필요할 수 있는 허용 후보
승인 후 구현 단계에서 필요할 수 있는 후보는 다음과 같다.
- `src/test/java/com/threestar/trainus/domain/comment/controller/**/*IntegrationTest.java`
- `src/test/java/com/threestar/trainus/domain/review/controller/**/*IntegrationTest.java`
- `src/test/java/com/threestar/trainus/global/config/security/**/*Test.java`
- `src/test/java/com/threestar/trainus/testsupport/**`
- 필요 시 `src/test/resources/application-test.yml`
- 필요 시 `build.gradle`

## 14. 수정 금지 파일
이번 승인 전후로도 수정하지 않는다.
- `src/main/java/**`
- `src/main/resources/**`
- 운영 인증 로직 변경
- 결제/S3/성능 테스트 추가
- `git push`
- PR 생성

## 15. 승인 요청이 필요한 이유
이번 후보는 단순한 테스트 추가가 아니라, 댓글/리뷰 컨트롤러 테스트가 실제 JWT filter chain 테스트로 전환되어야 하는지, 아니면 세션 주입 테스트를 보조용으로 남기고 JWT 기반 통합 테스트를 추가해야 하는지 결정해야 한다.

이 판단은 테스트 실행 비용, 인증 경계 해석, 기존 테스트 안정성, 그리고 세션 기반 테스트의 유지 여부에 직접 영향을 주므로 구현 전에 사용자의 승인 범위를 명확히 해야 한다.

## 16. 구현 전 사용자가 승인해야 할 최소 범위
사용자는 아래 최소 범위만 승인하면 된다.
- 댓글 생성/삭제와 리뷰 생성의 HTTP 인증 흐름을 JWT 기반 통합 테스트로 재정의할 것
- `Authorization` 헤더, JWT 발급 또는 테스트용 토큰 생성, `SecurityFilterChain` 통과 여부를 기준으로 검증할 것
- `PostgresIntegrationTestSupport`와 JWT test support를 활용하는 범위로만 진행할 것
- 세션 주입 테스트는 현재 인증 흐름 검증으로는 부족한 테스트로 분리해 설명할 것
- 결제/S3는 이번 작업에서 제외할 것

## 17. 검증 명령 후보
승인 후 구현이 생기면 다음 검증을 후보로 둔다.
- `./gradlew test --tests "*Comment*IntegrationTest"`
- `./gradlew test --tests "*Review*IntegrationTest"`
- `./gradlew test --tests "com.threestar.trainus.global.config.security.JwtAuthenticationFilterIntegrationTest"`
- `./gradlew test`

실제 실행은 승인 전에는 하지 않는다.

## 18. 최종 정리
현재 단계에서 승인 가능한 계획은 다음 한 줄로 요약된다.

댓글/리뷰 HTTP 인증 흐름은 세션 주입 테스트를 현재 인증 경계 검증에서 분리하고, JWT 기반 인증 흐름과 `SecurityFilterChain` 통과 여부를 기준으로 한 통합 테스트를 최소 범위로 추가하며, 결제/S3는 후속 후보로 분리한다.
