# P3 댓글/리뷰 JWT 인증 흐름 리뷰

검토 범위:
- [CommentControllerIntegrationTest.java](/Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/domain/comment/controller/CommentControllerIntegrationTest.java)
- [ReviewControllerIntegrationTest.java](/Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/domain/review/controller/ReviewControllerIntegrationTest.java)
- [JwtIntegrationTestSupport.java](/Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/testsupport/JwtIntegrationTestSupport.java)
- [PostgresIntegrationTestSupport.java](/Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/testsupport/PostgresIntegrationTestSupport.java)
- [SecurityConfig.java](/Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/main/java/com/threestar/trainus/global/config/security/SecurityConfig.java)
- [JwtAuthenticationFilter.java](/Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/main/java/com/threestar/trainus/global/config/security/JwtAuthenticationFilter.java)
- [LoginUserArgumentResolver.java](/Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/main/java/com/threestar/trainus/global/resolver/LoginUserArgumentResolver.java)

확인한 사항:
- 댓글/리뷰 통합 테스트에서 `MockHttpSession`, `LOGIN_USER`, `WithMockUser`, `session(...)` 기반 주입 흔적은 남아 있지 않았다.
- 두 테스트 모두 `Authorization: Bearer ...` 헤더를 넣고, 실제 `JwtProvider`로 만든 토큰을 사용한다.
- `SecurityConfig`는 `JwtAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 두고, `LoginUserArgumentResolver`는 `SecurityContextHolder`의 인증 객체를 읽는다.
- 테스트 support는 `PostgresIntegrationTestSupport`를 통해 PostgreSQL Testcontainers와 PostGIS를 사용한다.
- 댓글 생성, 댓글 삭제, 리뷰 생성의 성공 케이스와 무토큰/잘못된 토큰 실패 케이스가 포함되어 있다.

심각도 높은 지적:
1. `build.gradle` 변경이 이번 승인 범위 밖이다.
   - Testcontainers 의존성과 `test` 태스크의 Docker 환경 설정이 추가되어 있다.
   - 이번 요청에서는 `build.gradle`을 원칙적으로 제외하라고 했기 때문에, 이 변경은 별도 승인 근거가 없으면 범위 이탈이다.

중간 정도 리스크:
1. 댓글 삭제 테스트는 실제 삭제 직전에 댓글을 미리 `deleted=true`로 바꾼 뒤 삭제 API를 호출한다.
   - 현재 서비스는 이미 삭제된 댓글에 대해 사실상 no-op이므로, 이 테스트는 “활성 댓글 삭제”를 제대로 고정하지 못한다.
   - 인증 흐름은 보이지만, 삭제 동작 자체의 회귀 검증은 약하다.
2. 리뷰 생성 테스트는 JWT 인증 흐름은 검증하지만, 기존 세션 기반 테스트가 보던 프로필 메타데이터 갱신까지는 더 이상 확인하지 않는다.
   - 현재는 `content`, `rating` 위주로만 검증한다.
   - 리뷰 생성의 부수효과가 중요하다면 별도 보강이 필요하다.

낮은 수준의 리스크:
1. [application-test.yml](/Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/resources/application-test.yml) 변경은 테스트 부트스트랩 전역에 영향을 줄 수 있다.
   - 다만 현재 댓글/리뷰 통합 테스트는 `PostgresIntegrationTestSupport`가 주요 값을 다시 주입하므로 직접 영향은 제한적이다.
2. 병렬 `SpringBootTest` 실행은 `build/test-results` 기록 충돌과 Testcontainers 종료 시점 충돌을 일으킬 수 있다.
   - 단일 실행은 통과했지만, 병렬 실행은 동일한 방식으로 유지하면 불안정할 수 있다.

종합 판단:
- JWT 기반 인증 경로 자체는 현재 애플리케이션 구조와 잘 맞는다.
- 다만 `build.gradle` 범위 이탈, 댓글 삭제 테스트의 약한 삭제 검증, 리뷰 생성의 부수효과 검증 축소는 남은 리스크다.
