# P3 댓글/리뷰 HTTP 인증 흐름 조사 리포트

## 조사 범위
이 리포트는 이슈 #238의 P3 통합 테스트 후보 중 댓글/리뷰 HTTP 인증 흐름만 조사한 결과를 정리한 것이다.

조사 대상은 다음과 같다.
- `CommentControllerTest`
- `ReviewControllerTest`
- `SecurityConfig`
- `JwtAuthenticationFilter`
- `LoginUserArgumentResolver`
- JWT 관련 test support
- 기존 controller/security 테스트 구조

제외 대상은 결제와 S3이며, 이 문서에서는 후속 후보로만 분리했다.

## 확인한 파일 목록
### main
- `src/main/java/com/threestar/trainus/domain/comment/controller/CommentController.java`
- `src/main/java/com/threestar/trainus/domain/review/controller/ReviewController.java`
- `src/main/java/com/threestar/trainus/global/config/security/SecurityConfig.java`
- `src/main/java/com/threestar/trainus/global/config/security/JwtAuthenticationFilter.java`
- `src/main/java/com/threestar/trainus/global/config/security/JwtProvider.java`
- `src/main/java/com/threestar/trainus/global/resolver/LoginUserArgumentResolver.java`
- `src/main/java/com/threestar/trainus/global/config/WebConfig.java`
- `src/main/java/com/threestar/trainus/global/annotation/LoginUser.java`
- `src/main/java/com/threestar/trainus/domain/test/service/TestUserService.java`

### test
- `src/test/java/com/threestar/trainus/domain/comment/controller/CommentControllerTest.java`
- `src/test/java/com/threestar/trainus/domain/review/controller/ReviewControllerTest.java`
- `src/test/java/com/threestar/trainus/global/resolver/LoginUserArgumentResolverUnitTest.java`
- `src/test/java/com/threestar/trainus/global/config/security/JwtAuthenticationFilterIntegrationTest.java`
- `src/test/java/com/threestar/trainus/testsupport/PostgresIntegrationTestSupport.java`
- `src/test/java/com/threestar/trainus/domain/user/service/UserServiceTest.java`

## 핵심 관찰
### 1) 댓글/리뷰 컨트롤러는 `@LoginUser`를 사용한다
`CommentController`와 `ReviewController`는 모두 `@LoginUser Long ...` 파라미터를 받는다.

- 댓글 생성/삭제:
  - `CommentController`의 `createComment`, `deleteComment`
- 리뷰 생성:
  - `ReviewController`의 `createReview`

즉 컨트롤러 레벨에서는 “세션”이 아니라 “`@LoginUser` 해석 결과”가 사용자 식별자 주입의 기준이다.

### 2) 실제 `LoginUserArgumentResolver`는 세션이 아니라 `SecurityContextHolder`를 읽는다
`LoginUserArgumentResolver`는 다음 흐름으로 동작한다.

- `supportsParameter(...)`
  - `@LoginUser`가 붙은 `Long` 타입만 허용
- `resolveArgument(...)`
  - `SecurityContextHolder.getContext().getAuthentication()`을 읽음
  - 인증 정보가 없거나 인증되지 않았으면 `AUTHENTICATION_REQUIRED` 예외
  - principal이 `Long`이면 그대로 반환
  - 아니면 예외

즉, main 코드 기준으로는 `HttpSession`의 `"LOGIN_USER"` 값을 직접 읽는 구조가 아니다.

### 3) 댓글/리뷰 테스트는 `MockHttpSession`에 `LOGIN_USER`를 넣는 세션 주입 방식이다
`CommentControllerTest`와 `ReviewControllerTest`는 공통적으로:

- `@SpringBootTest`
- `@AutoConfigureMockMvc`
- `MockMvc`
- `MockHttpSession`

을 사용한다.

세션 주입 패턴은 다음과 같다.

- `session.setAttribute("LOGIN_USER", userId)`
- `mockMvc.perform(...).session(session)`

이 패턴은 실제 JWT 필터 체인을 태우는 방식이 아니라, 테스트 환경에서 세션 값을 통해 컨트롤러 호출을 성립시키는 형태로 보인다.

### 4) JWT filter chain 기반 테스트는 별도 통합 테스트가 이미 존재한다
`JwtAuthenticationFilterIntegrationTest`는 실제 Spring Security filter chain에서 JWT 인증이 동작하는지 확인한다.

확인된 흐름:

- `Authorization: Bearer <token>` 헤더를 요청에 실음
- `JwtAuthenticationFilter`가 토큰을 추출
- `JwtProvider.validateToken(...)`로 검증
- `JwtProvider.getAuthentication(...)`가 `UsernamePasswordAuthenticationToken(userId, null, authorities)`를 생성
- `SecurityContextHolder`에 인증 정보 주입

이 테스트는 세션이 아니라 Bearer 토큰과 보안 필터 체인을 검증한다.

### 5) `SecurityConfig`는 댓글/리뷰 GET을 permitAll로 열어두고, 필터는 전역 등록한다
`SecurityConfig`의 핵심은 다음이다.

- `JwtAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 등록
- `sessionCreationPolicy(STATELESS)`
- `csrf` 비활성화
- `GET /api/v1/comments/**`, `GET /api/v1/reviews/**`는 permitAll
- 그 외 요청은 인증 필요

즉 읽기 API는 인증 없이 접근 가능하고, 쓰기 API는 필터/시큐리티 컨텍스트가 살아 있어야 한다.

### 6) JWT test support는 토큰 생성과 통합 테스트 환경 주입에 집중되어 있다
`TestUserService`와 `PostgresIntegrationTestSupport`를 확인한 결과:

- `TestUserService.generateTokenCsvForTestUsers()`
  - 테스트 유저 목록을 읽어 JWT access token CSV 생성
- `PostgresIntegrationTestSupport`
  - Testcontainers PostgreSQL 환경 구성
  - `jwt.secret` 등 통합 테스트용 property 주입

이 구조는 JWT 기반 통합 테스트를 위한 기반이지, 세션 기반 인증을 테스트하려는 보조 도구는 아니다.

## 세션 주입 테스트와 JWT filter chain 테스트의 차이
### 세션 주입 기반 테스트
- 목적: 컨트롤러를 빠르게 호출하기 위한 테스트 데이터 주입
- 방식: `MockHttpSession`에 `"LOGIN_USER"` 저장
- 특징: 실제 JWT 검증과 필터 체인을 통과하지 않을 수 있음
- 현재 관찰: `CommentControllerTest`, `ReviewControllerTest`가 이 방식 사용

### JWT filter chain 기반 테스트
- 목적: 실제 보안 설정과 필터 체인 동작 검증
- 방식: `Authorization` 헤더에 Bearer token 전달
- 특징: `JwtAuthenticationFilter -> JwtProvider -> SecurityContextHolder` 흐름을 탐색
- 현재 관찰: `JwtAuthenticationFilterIntegrationTest`가 이 방식 사용

### 해석
현재 코드베이스에는 두 종류의 테스트가 분리되어 있다.

- 댓글/리뷰 컨트롤러 테스트는 세션 주입형
- JWT 인증 흐름은 별도 통합 테스트형

따라서 P3 후보를 잡을 때, “댓글/리뷰 인증 흐름”은 단순 세션 주입 테스트 추가가 아니라 실제로는 JWT filter chain을 타는 통합 테스트와의 정합성을 함께 봐야 한다.

## 결제/S3 제외 사유
이번 조사 범위는 댓글/리뷰 HTTP 인증 흐름이다.

결제와 S3는 다음 이유로 제외했다.

- 조사 목표가 댓글/리뷰 인증 흐름에 한정되어 있음
- 결제와 S3는 동일한 `@LoginUser`를 쓰더라도 도메인 검증과 외부 연동 성격이 달라 P3 후보 분리 기준을 흐릴 수 있음
- 현재 확보한 증거는 댓글/리뷰의 세션 주입 vs JWT 필터 체인 차이를 설명하는 데 충분하며, 결제/S3는 별도 비교가 필요함

## 후속 후보 분리
이번 조사 결과를 기준으로 후속 후보는 다음처럼 분리하는 것이 적절하다.

### 같은 축에서 계속 볼 후보
- 댓글 생성/삭제 통합 테스트
- 리뷰 생성 통합 테스트
- `@LoginUser` 주입이 실제 JWT 인증과 맞물리는지 확인하는 테스트

### 별도 축으로 분리할 후보
- 결제 HTTP 인증 흐름
- S3 업로드/다운로드 HTTP 인증 흐름

이 둘은 공통적으로 `@LoginUser`를 쓰더라도 외부 연동, 상태 저장, 보안 경계가 달라서 같은 묶음으로 처리하는 편이 덜 혼동된다.

## 결론
댓글/리뷰 컨트롤러 테스트는 현재 세션 주입형이며, main 코드의 `LoginUserArgumentResolver`는 `SecurityContextHolder` 기반이다. 반면 JWT 인증의 실제 흐름은 `SecurityConfig -> JwtAuthenticationFilter -> JwtProvider -> SecurityContextHolder`로 분리되어 있다.

따라서 P3 통합 테스트 후보를 정리할 때는 댓글/리뷰의 세션 주입 테스트와 JWT filter chain 테스트를 같은 의미로 취급하면 안 된다. 결제/S3는 이번 조사 범위 밖이므로 후속 후보로만 남겨 두는 것이 맞다.
