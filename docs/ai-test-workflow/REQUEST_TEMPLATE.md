# AI Test Workflow Request Examples

이 파일은 개인용 복사 예시다. 커밋 대상이 아니다.

## 1. Initial Request

```text
/goal 레슨 도메인 단위 테스트 공백 확인 및 커버리지 보강

docs/ai-test-workflow/AGENTS.md를 먼저 읽고 따른다.

작업 묶음: unit-test-coverage
작업 주제: lesson-domain
범위: 레슨 강사/수강생 도메인
제외: 통합 테스트, 운영 코드 변경

Researcher와 Planner는 반드시 별도 서브에이전트로 분리 실행한다.
Researcher 서브에이전트는 gpt-5.4-mini high로 실행해 research-report.md를 작성하게 한다.
Researcher 완료 후 Planner 서브에이전트는 gpt-5.4-mini high로 실행해 test-plan.md를 작성하게 한다.
Planner 완료 후 구현 전 멈추고 사용자 승인을 기다린다.
산출물은 docs/ai-test-workflow/reports/unit-test-coverage/cycles/lesson-domain/ 아래에 작성한다.
```

## 2. Approval Request

```text
/goal 승인된 범위 안에서 레슨 도메인 단위 테스트 구현 및 검증

docs/ai-test-workflow/AGENTS.md를 다시 확인하고 승인된 범위 안에서만 진행한다.

승인 범위:
- {approved_scope}

수정 허용 파일:
- {allowed_files}

수정 금지 파일:
- {forbidden_files}

검증 명령:
- {validation_commands}

메인 에이전트가 승인된 범위만 직접 구현한다.
구현과 검증 후 Reviewer 서브에이전트를 gpt-5.5 medium으로 반드시 별도 실행한다.
Reviewer 서브에이전트는 파일을 수정하지 않고 review-report.md만 작성한다.
리뷰 반영 후 cycle-summary.md와 pr-draft.md를 갱신한다.
최종 확인 전 PR은 생성하지 않는다.
```
