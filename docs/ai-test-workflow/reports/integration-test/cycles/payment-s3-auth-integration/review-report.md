# P3 후속 결제/S3/Auth 리뷰 보고서

## 결론
- 변경 사항은 승인 가능하다.
- 이번 사이클의 범위는 결제 prepare/confirm/cancel, S3 presigned URL 발급, JWT 인증 성공/실패 흐름으로 잘 고정되어 있다.

## 확인한 점
- 결제 테스트는 `PaymentClient` mock으로 외부 Toss 호출을 차단하고 있다.
- S3 테스트는 `AmazonS3` mock으로 실제 AWS 호출을 차단하고 있다.
- `JwtIntegrationTestSupport`를 재사용해 JWT 헤더 생성과 토큰 발급이 공용화되어 있다.
- `prepare / confirm / cancel`과 `posturl / geturl` 모두 성공과 실패 경계가 존재한다.
- 운영 코드 수정과 `build.gradle` 수정은 이번 사이클에서 없었다.

## 지적 사항
- `PaymentControllerIntegrationTest`의 cancel 경로는 현재 payment 취소와 participant 삭제를 중심으로 검증하고 있고, `TossPayment` 취소 상태까지는 고정하지 않는다.
- 이는 현재 승인 범위에서 치명적이지는 않지만, 후속 정밀화가 필요하면 별도 보강 후보가 된다.
- `MockBean` 관련 deprecation 경고가 컴파일 로그에 보이지만, 테스트 실패 원인은 아니다.

## 테스트 목적 적합성
- 결제 준비는 JWT 인증과 DB 저장을 함께 확인한다.
- 결제 승인과 취소는 외부 응답 경계를 mock으로 끊은 뒤 내부 상태 반영을 확인한다.
- S3는 presigned URL 발급 결과와 key 생성 규칙을 확인한다.
- JWT 실패 케이스는 각 controller에서 403으로 검증된다.

## 최종 판단
- 이번 변경은 승인 범위 안에서 동작하고, 외부 인프라 의존성도 통제되어 있다.
- 현재 상태로 머지 가능한 수준이다.
