---
tracker:
  kind: linear
  api_key: $LINEAR_API_KEY
  project_slug: "ai-test-workflow-94d799fcd978"
  required_labels:
    - ai-test-workflow
  active_states:
    - Backlog
    - Todo
    - Research
    - Approved
    - In Progress
    - In Review
    - Rework
  terminal_states:
    - Done
    - Closed
    - Cancelled
    - Canceled
    - Duplicate
polling:
  interval_ms: 5000
workspace:
  root: ~/code/symphony-workspaces/trainus
hooks:
  after_create: |
    git clone --branch develop https://github.com/prgrms-web-devcourse-final-project/WEB5_7_3star_BE.git .
agent:
  max_concurrent_agents: 1
  max_turns: 8
codex:
  command: codex --model gpt-5.4-mini --config model_reasoning_effort="high" --config shell_environment_policy.inherit=all app-server
  approval_policy: never
  thread_sandbox: workspace-write
  turn_sandbox_policy:
    type: workspaceWrite
    networkAccess: true
---

너는 Linear 이슈 `{{ issue.identifier }}`를 작업한다.

## 기본

- 이 파일은 실행 설정과 공통 규칙만 둔다.
- 세부 역할과 테스트 경계는 `docs/ai-test-workflow/rules/*.md`와 이슈 본문을 따른다.
- 근거가 없으면 추측하지 말고, 부족한 근거만 기록한다.
- 출력은 한국어로 쓴다.
- 코드 식별자, 파일 경로, 클래스명, 메서드명, 명령어, 예외명은 원문 그대로 유지한다.

## 산출물

- Researcher: `docs/ai-test-workflow/reports/{{ issue.identifier }}/research-report.md`
- Planner: `docs/ai-test-workflow/reports/{{ issue.identifier }}/test-plan.md`
- Reviewer: `docs/ai-test-workflow/reports/{{ issue.identifier }}/review-report.md`
- PR 초안: `docs/ai-test-workflow/reports/{{ issue.identifier }}/pr-draft.md`
- 로그: `docs/ai-test-workflow/logs/{{ issue.identifier }}/prompt-and-tool-log.md`

## 이슈 정보

- 식별자: `{{ issue.identifier }}`
- 제목: `{{ issue.title }}`
- 상태: `{{ issue.state }}`
- 라벨: `{{ issue.labels }}`
- URL: `{{ issue.url }}`

{% if issue.description %}
{{ issue.description }}
{% else %}
설명이 없습니다.
{% endif %}

## 첫 실행

- 작업공간에 `.codegraph/`가 없으면 `codegraph init -i`를 1회 실행한다.
- 인덱스가 있으면 재사용한다.
- 구조 질의는 CodeGraph를 우선 쓴다.

## 규칙

- 이슈를 단일 기준으로 삼는다.
- 이슈 범위를 벗어나지 않는다.
- 승인 전에는 운영 코드를 수정하지 않는다.
- 계획 작성 후 `Planning`으로 이동하고 종료한다.
- `Planning`은 active state가 아니며 승인 대기 중 재시도하지 않는다.
- 구현 완료 후 `In Review`로 이동한다.
- 리뷰 완료 후 `Final Approval`로 이동하고 종료한다.
- `Final Approval`은 active state가 아니며 승인 대기 중 재시도하지 않는다.
- `Done`, `Closed`, `Cancelled`, `Canceled`, `Duplicate`로 직접 전환하지 않는다.
- `git push`, `gh pr create`, PR merge는 하지 않는다.
- PR 본문은 초안만 남긴다.
- PR 초안은 `.github/PULL_REQUEST_TEMPLATE.md` 항목을 따른다.
- PR 초안의 관련 이슈는 GitHub 이슈 번호를 `#123` 형식으로 쓴다.
- `Final Approval`은 사람이 확인할 때만 쓰고 active state에는 넣지 않는다.
- 리뷰 반복 여부는 파일 존재가 아니라 현재 이슈, 작업 범위, 브랜치, 최신 커밋 기준으로 판단한다.
- 같은 스냅샷의 리뷰가 이미 있으면 요약만 남기고 `Final Approval`로 이동한다.

## 상태

- `Backlog`: 미착수
- `Todo`: 대기
- `Research`: 조사
- `Planning`: 계획, 승인 대기
- `Approved`: 승인 완료
- `In Progress`: 구현 중
- `In Review`: 검토 중
- `Rework`: 수정 중
- `Final Approval`: 최종 승인 대기
- `Done`: 최종 완료

## 최소 기록

- 리포트는 목적, 근거, 발견 사항, 한계, 다음 단계만 남긴다.
- 로그는 사용한 프롬프트와 도구만 기록한다.
- 산출물은 이슈별 디렉터리에 보관한다.
- 이전 이슈 산출물을 덮어쓰지 않는다.
- 최종 응답은 완료 작업, 수정 파일, 검증, 남은 블로커만 요약한다.
