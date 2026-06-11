# Enforcement Checklist

이 문서는 AI 에이전트에게 지침을 전달하는 데 그치지 않고, 테스트 품질 기준을 자동으로 검증할 수 있는 후보를 정리한다.

모든 항목을 한 번에 구현하는 것이 목표는 아니다. 우선순위가 높은 항목부터 적용한다.

## 1차 적용 후보

| 항목 | 강제 방식 | 목적 |
| --- | --- | --- |
| 테스트 profile 고정 | Gradle 또는 JUnit 검사 | `local`, `prod` profile 오사용 방지 |
| 통합 테스트 명명 규칙 | JUnit 검사 또는 ArchUnit | `*IntegrationTest` 기준 통일 |
| 단위 테스트의 Spring context 남용 방지 | JUnit 검사 | 빠른 단위 테스트 유지 |
| Redis/PostgreSQL 통합 테스트 환경 | Testcontainers | 로컬 인프라 의존 제거 |
| 테스트 실행 명령 분리 | Gradle `test`, `integrationTest` | 단위/통합 테스트 실행 경계 분리 |

## 2차 적용 후보

| 항목 | 강제 방식 | 목적 |
| --- | --- | --- |
| Controller -> Repository 직접 접근 금지 | ArchUnit | 레이어 경계 보호 |
| Service -> Controller 의존 금지 | ArchUnit | 역방향 의존 방지 |
| 테스트 데이터 cleanup 확인 | 공통 support class | 테스트 간 상태 오염 방지 |
| AI 작업 로그 작성 여부 | 문서 체크 script | 작업 추적성 확보 |
| CI 테스트 실행 | GitHub Actions | PR 단위 검증 자동화 |

## 적용 원칙

- 규칙은 문서화만 하지 않고 가능한 경우 테스트나 task로 실패하게 만든다.
- 단기 목표는 모든 규칙 강제가 아니라, 핵심 회귀 테스트가 안정적으로 반복 실행되는 구조를 만드는 것이다.
- 강제 규칙이 테스트 작성 속도를 과도하게 늦추면 우선순위를 조정한다.

