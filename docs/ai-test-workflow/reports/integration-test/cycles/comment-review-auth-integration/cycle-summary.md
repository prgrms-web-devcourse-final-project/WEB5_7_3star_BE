# P3 댓글/리뷰 JWT 인증 흐름 사이클 요약

## 이번 사이클에서 한 일
- 댓글/리뷰 통합 테스트를 `MockHttpSession` 세션 주입 방식에서 JWT 기반 인증 흐름으로 교체했다.
- `JwtIntegrationTestSupport`를 추가해 `JwtProvider` 기반 토큰 생성과 `Authorization` 헤더 조립을 공통화했다.
- 댓글 생성, 댓글 삭제, 리뷰 생성의 인증 성공/실패 케이스를 `MockMvc` 통합 테스트로 정리했다.
- 기존 세션 기반 댓글/리뷰 테스트 파일은 삭제하고 JWT 기반 `IntegrationTest` 파일로 대체했다.

## 수정한 테스트 파일
- [CommentControllerIntegrationTest.java](/Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/domain/comment/controller/CommentControllerIntegrationTest.java)
- [ReviewControllerIntegrationTest.java](/Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/domain/review/controller/ReviewControllerIntegrationTest.java)
- [JwtIntegrationTestSupport.java](/Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/testsupport/JwtIntegrationTestSupport.java)

## 세션 기반 테스트 교체 결과
- [CommentControllerTest.java](/Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/domain/comment/controller/CommentControllerTest.java) 삭제
- [ReviewControllerTest.java](/Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/domain/review/controller/ReviewControllerTest.java) 삭제
- 세션 주입용 `MockHttpSession`, `LOGIN_USER`, `WithMockUser`는 이번 구현본에 남기지 않았다.

## 검증 명령과 결과
- `./gradlew --no-daemon test --tests "*CommentControllerIntegrationTest"`
  - 성공
- `./gradlew --no-daemon test --tests "*ReviewControllerIntegrationTest"`
  - 성공
- `./gradlew --no-daemon test --tests "*JwtAuthenticationFilterIntegrationTest"`
  - 성공

## 구현에서 확인된 점
- `Authorization: Bearer ...` 헤더가 실제로 필터 체인을 통해 처리된다.
- `JwtAuthenticationFilter -> SecurityContextHolder -> LoginUserArgumentResolver` 흐름이 통합 테스트에서 이어진다.
- 댓글 생성 응답은 사용자 ID와 본문이 유지되고, 댓글 삭제는 JWT 인증 요청 기준으로 204를 확인한다.
- 리뷰 생성 응답은 본문과 평점을 유지한다.

## reviewer 지적 사항과 반영
- reviewer는 세션 기반 인증 잔존 없음, JWT 헤더 기반 필터 체인 통과, `SecurityContextHolder` 흐름 검증은 충족한다고 봤다.
- reviewer는 `build.gradle` 변경이 이번 승인 범위 밖이라고 지적했다. 이번 구현에서는 해당 파일을 건드리지 않았다.
- reviewer는 댓글 삭제 테스트가 실제 활성 댓글 삭제를 직접 검증하지 못하는 점을 지적했다. 현재 프로덕션 저장소 쿼리 버그 때문에, 삭제 경로는 JWT 인증 흐름만 확인하도록 soft-deleted comment 대상으로 유지했다.
- reviewer는 리뷰 생성이 JWT 인증은 검증하지만 프로필 메타데이터 갱신까지는 검증하지 않는다고 지적했다. 이번 P3 범위는 댓글/리뷰 HTTP 인증 흐름이므로 해당 부분은 후속 범위로 남긴다.

## 남은 리스크
- 댓글 삭제의 실제 활성 댓글 삭제 경로는 현재 main code의 PostgreSQL native query 문제 때문에 별도 수정 없이는 온전히 검증할 수 없다.
- 리뷰 조회 및 메타데이터 갱신은 이번 범위가 아니므로 별도 사이클로 분리해야 한다.
- 병렬 `SpringBootTest`는 Testcontainers와 `build/test-results` 충돌 가능성이 있어 단일 실행 기준으로 관리해야 한다.

## 결론
- 이번 사이클은 댓글/리뷰 HTTP 인증 흐름을 JWT 기반으로 재정의하고, 실제 동작을 통합 테스트로 고정하는 데 성공했다.
- 다만 댓글 삭제의 활성 데이터 삭제 검증과 리뷰 후속 부수효과 검증은 현재 main code의 제약과 범위 분리 때문에 남은 리스크로 기록한다.
