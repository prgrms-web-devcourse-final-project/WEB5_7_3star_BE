# P3 후속 결제/S3/Auth 통합 테스트 계획

## 1. 계획 기준
- 이 계획은 Researcher 결과만 근거로 작성한다.
- 실제 AWS S3 호출은 제외한다.
- 실제 Toss Payments 호출은 제외한다.
- S3는 `AmazonS3` mock 기반의 서버 내부 presigned URL 발급 흐름만 검증한다.
- 결제는 mock/stub 기반의 승인/실패 경계와 DB 반영 흐름만 검증한다.
- JWT는 `JwtAuthenticationFilter`, `LoginUserArgumentResolver`, `@LoginUser`가 붙은 HTTP 흐름에서 같이 검증한다.
- CORS 중심 검증, 성능 테스트, 운영 코드 수정은 이번 사이클 범위에서 제외한다.

## 2. 현재 공백
- `src/test/java/com/threestar/trainus/domain/payment` 아래 테스트가 비어 있다.
- `src/test/java/com/threestar/trainus/domain/file` 아래 테스트가 비어 있다.
- JWT 기반 통합 테스트는 댓글/리뷰 도메인에만 있다.

## 3. 우선순위별 계획

### P1. 결제 HTTP/API 통합 테스트
결제는 이번 후속 사이클의 최우선 후보로 둔다. 실제 외부 결제 호출은 하지 않고, 승인/실패 응답 경계와 DB 반영만 본다.

| 후보 | 통합 테스트에서 볼 것 | 수정 허용 파일 | 수정 금지 파일 | 검증 명령 후보 |
|---|---|---|---|---|
| `POST /api/v1/payments/prepare` | JWT 인증 성공/실패, 준비 응답, 결제 준비 상태 반영, 외부 결제 클라이언트 mock/stub | `src/test/java/com/threestar/trainus/domain/payment/**`<br>`src/test/java/com/threestar/trainus/testsupport/**`<br>`docs/ai-test-workflow/reports/integration-test/cycles/payment-s3-auth-integration/**` | `src/main/**`<br>`build.gradle`<br>실제 Toss 설정/호출 경로 | `./gradlew --no-daemon test --tests '*Payment*IntegrationTest'` |
| `POST /api/v1/payments/confirm` | 승인 응답 경계, 실패 응답 경계, DB 반영, JWT 인증 경계 | 동일 | 동일 | `./gradlew --no-daemon test --tests '*Payment*IntegrationTest'` |
| `POST /api/v1/payments/cancel` | 취소 응답 경계, 취소 실패 경계, DB 반영, JWT 인증 경계 | 동일 | 동일 | `./gradlew --no-daemon test --tests '*Payment*IntegrationTest'` |
| `GET /view/success` | 결제 성공 view 응답 경계, 인증이 필요한 경우 401/403 포함 | 동일 | 동일 | `./gradlew --no-daemon test --tests '*Payment*IntegrationTest'` |
| `GET /view/cancel` | 결제 취소 view 응답 경계, 인증이 필요한 경우 401/403 포함 | 동일 | 동일 | `./gradlew --no-daemon test --tests '*Payment*IntegrationTest'` |

### P2. S3 Presigned URL 발급 통합 테스트
S3는 실제 업로드 검증이 아니라 서버 내부 presigned URL 발급 흐름만 본다. `AmazonS3` mock이 전제다.

| 후보 | 통합 테스트에서 볼 것 | 수정 허용 파일 | 수정 금지 파일 | 검증 명령 후보 |
|---|---|---|---|---|
| `GET /api/v1/s3/posturl` | `AmazonS3` mock 기반 presigned PUT URL 생성, 경로 규칙, 만료 규칙, JWT 인증 경계 | `src/test/java/com/threestar/trainus/domain/file/**`<br>`src/test/java/com/threestar/trainus/testsupport/**`<br>`docs/ai-test-workflow/reports/integration-test/cycles/payment-s3-auth-integration/**` | `src/main/**`<br>`build.gradle`<br>실제 AWS 호출 경로 | `./gradlew --no-daemon test --tests '*S3*IntegrationTest'` |
| `GET /api/v1/s3/geturl` | `AmazonS3` mock 기반 presigned GET URL 생성, 경로 규칙, 만료 규칙, JWT 인증 경계 | 동일 | 동일 | `./gradlew --no-daemon test --tests '*S3*IntegrationTest'` |

### P3. JWT 공통 경계 및 단위 테스트
JWT는 payment/S3 HTTP 흐름의 공통 전제이고, 개별 단위 테스트로 충분한 항목은 별도 분리한다.

| 후보 | 범위 | 수정 허용 파일 | 수정 금지 파일 | 검증 명령 후보 |
|---|---|---|---|---|
| `JwtProvider` | access token 생성/검증 규칙 | `src/test/java/com/threestar/trainus/global/config/security/**` | `src/main/**`<br>`build.gradle`<br>외부 인프라 | `./gradlew --no-daemon test --tests '*JwtProviderTest'` |
| `PaymentClient` | 헤더 구성, 오류 매핑 | `src/test/java/com/threestar/trainus/domain/payment/**` | 동일 | `./gradlew --no-daemon test --tests '*PaymentClientTest'` |
| `PaymentMapper` | DTO/응답 매핑 | 동일 | 동일 | `./gradlew --no-daemon test --tests '*PaymentMapperTest'` |
| `RefundPolicyUtils` | 환불 정책 분기 | 동일 | 동일 | `./gradlew --no-daemon test --tests '*RefundPolicyUtilsTest'` |
| `S3Service` | 경로 생성 규칙, 만료 생성 규칙 | `src/test/java/com/threestar/trainus/domain/file/**` | 동일 | `./gradlew --no-daemon test --tests '*S3ServiceTest'` |

## 4. fake/mock/local stub으로 검증 가능한 후보
- 결제 승인/실패 응답은 실제 Toss 대신 mock/stub으로 검증한다.
- S3 presigned URL 발급은 실제 AWS 대신 `AmazonS3` mock으로 검증한다.
- JWT는 실제 서명된 토큰을 테스트 지원 코드로 생성하되, 외부 인증 서버는 두지 않는다.
- DB 반영은 로컬 테스트 DB에서만 확인한다.

## 5. 실제 외부 인프라가 필요해 보류해야 할 후보
- 실제 AWS S3 업로드 호출.
- 실제 Toss Payments 호출.
- CORS 중심의 브라우저 연동 검증.
- 성능 테스트.

## 6. 이번 사이클에서 구현하면 안 되는 항목
- `src/main/**` 수정.
- `build.gradle` 수정.
- 실제 AWS S3 호출을 위한 설정 추가.
- 실제 Toss API 호출을 위한 설정 추가.
- 성능 테스트 추가.
- CORS 전용 검증 추가.
- 댓글/리뷰 도메인 범위를 다시 건드리는 작업.

## 7. 다음 구현 승인 요청에 사용할 수정 허용 파일 목록
다음 승인을 요청할 때는 아래 범위만 열어 두는 것이 적절하다.

- `src/test/java/com/threestar/trainus/domain/payment/controller/PaymentControllerIntegrationTest.java`
- `src/test/java/com/threestar/trainus/domain/payment/service/PaymentServiceIntegrationTest.java`
- `src/test/java/com/threestar/trainus/domain/file/controller/S3ControllerIntegrationTest.java`
- `src/test/java/com/threestar/trainus/domain/file/service/S3ServiceIntegrationTest.java`
- `src/test/java/com/threestar/trainus/testsupport/JwtIntegrationTestSupport.java`
- `src/test/java/com/threestar/trainus/testsupport/PaymentIntegrationTestSupport.java`
- `src/test/java/com/threestar/trainus/testsupport/S3IntegrationTestSupport.java`
- `src/test/java/com/threestar/trainus/domain/payment/**` 아래의 단위 테스트 파일
- `src/test/java/com/threestar/trainus/domain/file/**` 아래의 단위 테스트 파일
- `docs/ai-test-workflow/reports/integration-test/cycles/payment-s3-auth-integration/**`
