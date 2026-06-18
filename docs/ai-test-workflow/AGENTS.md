# AI Test Workflow Guide

이 문서는 TrainUs 테스트 작업에서 모든 AI 에이전트와 작업자가 먼저 읽어야 하는 최상위 규칙이다.

## Goal

- 테스트 작업을 조사, 계획, 승인, 구현, 리뷰, 최종 확인 단계로 분리한다.
- 테스트 종류에 맞는 실행 경계를 유지한다.
- 승인되지 않은 코드 변경과 범위 확장을 막는다.
- 프롬프트, 도구 사용, 모델, 토큰 사용량을 추적 가능한 산출물로 기록한다.

## Required Reading Order

1. `docs/ai-test-workflow/AGENTS.md`
2. `docs/ai-test-workflow/rules/agent-roles.md`
3. `docs/ai-test-workflow/rules/human-approval-policy.md`
4. `docs/ai-test-workflow/rules/test-boundary.md`
5. `docs/ai-test-workflow/rules/enforcement-checklist.md`

## Report Structure

테스트 작업 산출물은 작업 주제 단위로 관리한다.

```text
docs/ai-test-workflow/reports/{workstream}/
  pr-draft.md
  cycles/
    {cycle}/
      research-report.md
      test-plan.md
      review-report.md
      cycle-summary.md
docs/ai-test-workflow/logs/{workstream}/
  campaign-log.md
  cycles/
    {cycle}/
      prompt-and-tool-log.md
```

## Cycle

각 작업 주제는 같은 사이클을 반복한다.

```text
test task request
>>> Researcher report
>>> Planner report
>>> human approval
>>> implementation
>>> Reviewer report
>>> fixes
>>> cycle summary
>>> final PR draft update
>>> manual PR
```

## Approval Gates

- Gate 1: Planner가 `test-plan.md`를 작성한 뒤 구현 전 사용자 승인 대기
- Gate 2: Reviewer가 `review-report.md`를 작성한 뒤 최종 반영 전 사용자 확인 대기
- Gate 3: `pr-draft.md` 작성 후 사용자가 수동 PR 진행 여부 결정

## Stop Points

- Researcher는 `research-report.md` 작성 후 종료한다.
- Planner는 `test-plan.md`에 승인 요청 범위를 작성한 뒤 종료한다.
- Implementer는 승인된 범위 구현과 검증 결과 기록 후 종료한다.
- Reviewer는 `review-report.md` 작성 후 파일 수정 없이 종료한다.
- Main agent는 사용자의 다음 승인이 필요한 시점에서 작업을 멈춘다.

## Role Policy

- Main agent: 오케스트레이션, 사용자 승인 확인, 구현, 테스트 실행, 최종 정리
- Researcher: 조사만 수행, 코드 수정 금지
- Planner: 계획만 작성, 코드 수정 금지
- Reviewer: 리뷰만 수행, 코드 수정 금지
- Implementer: 승인 후 구현 담당

## Approval Policy

- 사용자 승인 전 구현 금지
- 승인 전 운영 코드 수정 금지
- 승인 범위 밖 파일 수정 금지
- 테스트 실패를 운영 코드 변경으로 우회 금지
- 리뷰 후 사용자가 최종 확인하기 전 수동 PR 진행 금지

## CodeGraph

- 구조 조사 시작 전 CodeGraph 상태 확인
- `.codegraph/`가 없으면 사용자 승인 후 `codegraph init -i` 실행
- 심볼 위치, 호출 관계, 영향 범위는 CodeGraph 우선 사용
- 문자열 검색, 주석, 로그 메시지는 `rg` 사용

## Record Policy

- 각 사이클 보고서는 `reports/{workstream}/cycles/{cycle}/`에 작성한다.
- 로그는 `logs/{workstream}/cycles/{cycle}/`에 작성한다.
- 이전 사이클 산출물은 덮어쓰지 않는다.
- 로그에는 프롬프트, 사용 도구, 모델명, reasoning effort, 단계별 토큰 사용량을 기록한다.
- 모델은 구체적인 모델명과 실행 설정으로 기록한다.
- PR은 자동 생성하지 않고 `reports/{workstream}/pr-draft.md`에 초안만 작성한다.
- PR 초안은 `.github/PULL_REQUEST_TEMPLATE.md`를 참고한다.
