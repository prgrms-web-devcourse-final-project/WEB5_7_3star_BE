# P3 후속 결제/S3/Auth 사이클 요약

## 이번 사이클에서 한 일
- 결제 통합 테스트를 `POST /api/v1/payments/prepare`, `confirm`, `cancel` 기준으로 추가했다.
- S3 Presigned URL 통합 테스트를 `GET /api/v1/s3/posturl`, `GET /api/v1/s3/geturl` 기준으로 추가했다.
- JWT 공용 support인 `JwtIntegrationTestSupport`를 재사용해 `Authorization: Bearer ...` 헤더 조립을 공통화했다.
- 결제 외부 호출은 `PaymentClient` mock으로, S3 외부 호출은 `AmazonS3` mock으로 격리했다.
- `success / cancel view` 흐름은 이번 범위에서 제외했다.

## 수정한 테스트 파일
- [PaymentControllerIntegrationTest.java](/Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/domain/payment/controller/PaymentControllerIntegrationTest.java)
- [S3ControllerIntegrationTest.java](/Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/domain/file/controller/S3ControllerIntegrationTest.java)
- [JwtIntegrationTestSupport.java](/Users/minhyeok/Desktop/project/Dev-Course/WEB5_7_3star_BE/src/test/java/com/threestar/trainus/testsupport/JwtIntegrationTestSupport.java)

## 검증 명령과 결과
- `./gradlew --no-daemon test --tests '*Payment*IntegrationTest'`
  - 성공
- `./gradlew --no-daemon test --tests '*S3*IntegrationTest'`
  - 성공
- `./gradlew --no-daemon test --tests '*JwtAuthenticationFilterIntegrationTest'`
  - 성공

## 구현에서 확인된 점
- 결제 준비는 JWT 인증 성공/실패를 분리해 검증했고, READY 결제 저장을 DB에서 확인했다.
- 결제 승인은 Toss 응답을 mock으로 고정하고, Payment와 participant 상태 반영을 확인했다.
- 결제 취소는 Toss 취소 응답을 mock으로 고정하고, Payment 취소와 participant 삭제를 확인했다.
- S3 업로드용 presigned URL은 PUT method, bucket, key prefix/패턴, 만료 시점을 확인했다.
- S3 조회용 presigned URL은 GET method, bucket, key, 만료 시점을 확인했다.
- JWT 인증 실패는 각 API에서 403으로 고정했다.

## reviewer 지적 사항과 반영
- reviewer는 실제 Toss API 호출이 없고, 실제 AWS S3 호출이 없으며, JWT support를 재사용한 점을 확인했다.
- reviewer는 결제 취소 테스트가 TossPayment 취소 상태까지 고정하지 않는 점을 낮은 위험으로 봤다. 이번 범위에서는 payment 취소와 participant 삭제를 기준으로 유지했다.
- reviewer는 운영 코드 수정과 build.gradle 변경이 없다는 점을 확인했다.

## 남은 블로커
- 기능적으로는 없다.
- 다만 테스트 종료 시 Testcontainers/Hibernate shutdown 경고가 출력될 수 있어, 판단은 `BUILD SUCCESSFUL`과 개별 테스트 결과 기준으로 해야 한다.

## 결론
- 이번 사이클은 결제/S3/JWT 후속 통합 테스트 후보를 실제 통합 테스트로 구현하고, 개별 실행 기준으로 모두 통과시켰다.
