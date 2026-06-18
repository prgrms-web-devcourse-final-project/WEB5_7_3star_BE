# P3 후속 결제/S3/Auth 통합 테스트 후보 조사 보고서

## 1. 조사 범위와 전제
- 이번 조사는 `결제`, `S3 Presigned URL`, `JWT 인증` 흐름의 통합 테스트 후보를 찾는 목적이다.
- 구현, 운영 코드 수정, `build.gradle` 수정, 실제 AWS S3 호출, 실제 Toss Payments 호출, 성능 테스트는 제외했다.
- 프론트 CORS는 핵심 범위가 아니다.
- S3는 `PublicRead` 제거 이후의 현재 코드 상태를 기준으로 봤다.
- 결제는 실제 외부 Toss 호출 없이 mock 또는 local stub 경계를 기준으로 봤다.

## 2. 현재 코드에서 확인된 핵심 흐름

### 2-1. JWT 인증 흐름
- `SecurityConfig.filterChain`에서 `JwtAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 둔다.
- `JwtAuthenticationFilter`는 `Authorization` 헤더에서 `Bearer` 토큰을 꺼내 `JwtProvider.validateToken()`과 `JwtProvider.getAuthentication()`을 호출한다.
- `LoginUserArgumentResolver`는 `SecurityContextHolder`의 `Authentication`에서 principal이 `Long`인지 확인한 뒤 `@LoginUser` 파라미터를 채운다.
- `SecurityConfig`에서 `GET /api/v1/comments/**`, `GET /api/v1/reviews/**`만 예외로 열려 있고, 그 외 요청은 기본적으로 인증이 필요하다.

### 2-2. 결제 흐름
- `PaymentController`의 주요 엔드포인트는 다음과 같다.
  - `POST /api/v1/payments/prepare`
  - `POST /api/v1/payments/saveAmount`
  - `POST /api/v1/payments/verifyAmount`
  - `POST /api/v1/payments/confirm`
  - `POST /api/v1/payments/cancel`
  - `GET /api/v1/payments/view/success`
  - `GET /api/v1/payments/view/cancel`
- `PaymentService.preparePayment()`는 lesson, user, payment repository, participant 검증, 중복 결제 검증만 사용한다.
- `PaymentService.processConfirm()`와 `processCancel()`는 `PaymentClient`를 통해 Toss 경계와 연결된다.
- `PaymentService.viewAllSuccessTransaction()`과 `viewAllFailureTransaction()`은 DB 조회와 매퍼 조합으로 결과를 만든다.
- `PaymentClient`는 `RestClient`로 Toss API를 호출한다. 실제 Toss 호출은 이번 범위에서 제외다.

### 2-3. S3 Presigned URL 흐름
- `S3Controller`의 주요 엔드포인트는 다음과 같다.
  - `GET /api/v1/s3/posturl`
  - `GET /api/v1/s3/geturl`
- `S3Service.getPostS3Url()`은 `AmazonS3.generatePresignedUrl()`로 PUT presigned URL을 만든다.
- `S3Service.getGetS3Url()`은 `AmazonS3.generatePresignedUrl()`로 GET presigned URL을 만든다.
- 현재 코드에는 `PublicRead`, `withCannedAcl`, `setCannedAcl` 같은 ACL 설정 코드가 없다.
- 따라서 이번 검토는 업로드/조회용 URL 생성과 반환값 검증에 집중해야 한다.

## 3. 현재 테스트 공백
- `src/test/java/com/threestar/trainus/domain/payment` 아래 테스트 파일이 없다.
- `src/test/java/com/threestar/trainus/domain/file` 아래 테스트 파일이 없다.
- 현재 JWT 기반 통합 테스트는 댓글/리뷰 영역에만 보인다.
  - `CommentControllerIntegrationTest`
  - `ReviewControllerIntegrationTest`
- 즉, 결제와 S3는 인증 경계까지 포함한 HTTP 통합 테스트가 비어 있다.

## 4. 후보 분류

### 4-1. 결제 도메인

#### 통합 테스트 후보
| 후보 | 검증 방식 | 외부 인프라 의존 | 판단 |
|---|---|---:|---|
| `POST /api/v1/payments/prepare` | JWT + DB + 서비스 흐름 통합 | 없음 | 우선 후보 |
| `POST /api/v1/payments/confirm` | JWT + DB + `PaymentClient` local stub | Toss 실제 호출 없음 | 우선 후보 |
| `POST /api/v1/payments/cancel` | JWT + DB + `PaymentClient` local stub | Toss 실제 호출 없음 | 우선 후보 |
| `GET /api/v1/payments/view/success` | DB 조회, 페이징, 매핑 검증 | 없음 | 우선 후보 |
| `GET /api/v1/payments/view/cancel` | DB 조회, 페이징, 매핑 검증 | 없음 | 우선 후보 |

#### 단위 테스트로 충분한 항목
- `PaymentClient`의 헤더 생성과 오류 매핑.
- `PaymentMapper`의 DTO 변환 함수.
- `RefundPolicyUtils` 같은 순수 계산 유틸.
- `PaymentService` 내부의 단순 계산 또는 분기 로직 가운데 DB, HTTP, SecurityContext가 없는 부분.

#### 보류해야 할 항목
- 실제 Toss 승인/취소 API 호출.
- 외부 결제 사업자와의 실연동 E2E 흐름.

#### 해석
- 결제의 핵심 통합 테스트는 `prepare`로 내부 검증 흐름을 고정하고, `confirm/cancel`은 local stub으로 외부 경계를 잘라 DB 상태 변화를 확인하는 쪽이 적절하다.
- 조회 계열은 외부 API가 없으므로 DB 통합 테스트 성격이 강하다.

### 4-2. S3 Presigned URL 도메인

#### 통합 테스트 후보
| 후보 | 검증 방식 | 외부 인프라 의존 | 판단 |
|---|---|---:|---|
| `GET /api/v1/s3/posturl` | JWT + `S3Service` + mocked `AmazonS3` | AWS 호출 없음 | 우선 후보 |
| `GET /api/v1/s3/geturl` | JWT + `S3Service` + mocked `AmazonS3` | AWS 호출 없음 | 우선 후보 |

#### 단위 테스트로 충분한 항목
- `S3Service`의 경로 생성 규칙.
- `S3Service`의 만료 시간 생성 규칙.
- `GetS3UrlDto` 매핑.

#### 보류해야 할 항목
- 실제 AWS S3 업로드/다운로드.
- 실제 버킷 객체 생성 여부를 확인하는 네트워크 테스트.
- CORS 중심 검증.

#### 해석
- 현재 코드는 presigned URL 발급만 수행하므로, AWS 네트워크 없이 `AmazonS3.generatePresignedUrl()` 반환값만 local stub으로 고정하면 충분하다.
- `posturl`은 `image/{userId}` prefix와 UUID가 섞인 key 패턴을 검증하는 방향이 맞다.
- `PublicRead` ACL이 제거된 현재 코드에서는 ACL 검증보다 `HTTP method`, `bucket`, `key`, `expiration`, `응답 DTO` 검증이 더 중요하다.

### 4-3. JWT 인증 도메인

#### 통합 테스트 후보
| 후보 | 검증 방식 | 외부 인프라 의존 | 판단 |
|---|---|---:|---|
| `JwtAuthenticationFilter` | valid/missing/expired/malformed 토큰에 대한 HTTP 경계 검증 | 없음 | 우선 후보 |
| `LoginUserArgumentResolver` | `SecurityContextHolder` principal 주입 검증 | 없음 | 우선 후보 |
| `@LoginUser`가 붙은 payment/S3 HTTP 엔드포인트 | JWT 성공/실패에 따른 200/401/403 경계 검증 | 없음 | 우선 후보 |

#### 단위 테스트로 충분한 항목
- `JwtProvider.createAccessToken()`
- `JwtProvider.validateToken()`
- `JwtProvider.getAuthentication()`

#### 보류해야 할 항목
- 프론트와의 CORS 경계만 따로 확인하는 테스트.
- 외부 IdP 또는 실제 토큰 발급 서버 연동.

#### 해석
- JWT는 결제와 S3 테스트에서 공통 전제다.
- 이미 댓글/리뷰 쪽에서 같은 패턴의 JWT 기반 통합 테스트가 존재하므로, 이번 후속 범위에서는 payment/S3 쪽에 같은 패턴을 확장하는 것이 자연스럽다.

## 5. 실제 외부 인프라 없이 검증 가능한 방식
- 결제는 `PaymentClient`를 mock 또는 local stub으로 바꾸고, DB 반영과 응답 바디를 검증한다.
- S3는 `AmazonS3`를 mock으로 두고, presigned URL 요청 파라미터와 DTO 반환값을 검증한다.
- JWT는 실제 `JwtProvider`를 사용하되, 토큰 생성과 인증 필터 결과만 내부에서 검증한다.
- 공통적으로 `MockMvc`, Testcontainers 기반 PostgreSQL, repository assertion 조합이 가장 현실적이다.

## 6. 이번 사이클에서 구현하면 안 되는 항목
- `src/main/**` 수정
- `build.gradle` 수정
- 실제 AWS S3 호출
- 실제 Toss Payments 호출
- 성능 테스트
- CORS 중심 회귀 테스트

## 7. 핵심 후보 요약
1. `POST /api/v1/payments/prepare`는 JWT와 DB 검증을 함께 고정할 수 있는 1순위 후보다.
2. `POST /api/v1/payments/confirm`와 `POST /api/v1/payments/cancel`는 Toss를 local stub으로 끊고 DB 상태 변화를 확인하는 1순위 후보다.
3. `GET /api/v1/s3/posturl`과 `GET /api/v1/s3/geturl`은 AWS 없이 `AmazonS3` mock으로 검증 가능한 1순위 후보다.
4. `JwtAuthenticationFilter`와 `LoginUserArgumentResolver`는 payment/S3 HTTP 테스트의 공통 인증 전제라서 별도 통합 테스트 후보로 분리하는 편이 좋다.
5. `PaymentClient`와 실제 AWS/Toss 연동은 이번 사이클의 통합 테스트 대상이 아니라 보류 항목이다.
