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
    - Planning
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

너는 Linear 이슈 `{{ issue.identifier }}`를 작업하고 있다.

이 파일은 실행 설정과 공통 지시만 담는다. 세부 역할, 승인 기준, 테스트 경계는 이슈 본문과 `docs/ai-test-workflow/rules/*.md`에서 정한다.

산출물 경로:
- Researcher: `docs/ai-test-workflow/reports/research-report.md`
- Planner: `docs/ai-test-workflow/reports/test-plan.md`
- Reviewer: `docs/ai-test-workflow/reports/review-report.md`
- 공통 로그: `docs/ai-test-workflow/logs/prompt-and-tool-log.md`

이슈 정보:
- 식별자: {{ issue.identifier }}
- 제목: {{ issue.title }}
- 현재 상태: {{ issue.state }}
- 라벨: {{ issue.labels }}
- URL: {{ issue.url }}

설명:
{% if issue.description %}
{{ issue.description }}
{% else %}
설명이 없습니다.
{% endif %}

참조 문서:
- `docs/ai-test-workflow/rules/agent-roles.md`가 있으면 읽는다.
- `docs/ai-test-workflow/rules/human-approval-policy.md`가 있으면 읽는다.
- `docs/ai-test-workflow/rules/test-boundary.md`가 있으면 읽는다.
- 근거가 부족할 때는 임의로 만들지 말고, 부족한 근거를 기록한 뒤 가능한 범위의 분석만 계속한다.

출력 규칙:
- 이슈 결과, 리포트 파일, 로그 항목, Linear 코멘트, 최종 응답은 모두 한국어로 작성한다.
- 코드 식별자, 파일 경로, 클래스명, 메서드명, 명령어, 예외명은 원문 그대로 유지한다.

작업 원칙:
1. Linear 이슈를 작업의 단일 기준으로 삼는다.
2. 이슈 본문에 적힌 역할과 범위를 따른다.
3. 이슈 범위를 벗어나지 않는다.
4. 이슈에 사람 승인이 필요하면, 요청된 리포트 또는 계획만 만든 뒤 멈춘다.
5. Implementer 역할이 명시되고 승인이 완료되지 않으면 운영 코드를 수정하지 않는다.
6. 이슈가 워크플로 증거를 요구하면 `docs/ai-test-workflow/logs/prompt-and-tool-log.md`에 프롬프트/도구 사용 내역을 기록한다.
7. 최종 응답에는 완료한 작업, 수정한 파일, 실행한 검증, 남은 블로커를 요약한다.
8. 리포트를 작성할 때는 `목적, 확인한 근거, 발견 사항, 한계/블로커, 다음 단계`처럼 간결한 한국어 섹션을 우선 사용한다.
9. agent는 Linear 이슈를 `Done`, `Closed`, `Cancelled`, `Canceled`, `Duplicate` 같은 terminal state로 변경하지 않는다. terminal state 전환은 사람만 수행한다.
10. 구현 완료 후에는 `In Review` 상태까지만 전환할 수 있다. 리뷰 완료 후에도 `Done`으로 닫지 말고 리뷰 리포트와 Linear 코멘트에 최종 확인 요청만 남긴다.
11. `git push`, `gh pr create`, PR 생성, PR merge, remote branch 삭제는 사람이 명시적으로 승인하기 전까지 수행하지 않는다.
12. PR 생성은 이슈 본문 또는 최신 Linear 코멘트에 `PR_CREATE_APPROVED` 문구가 명시되어 있을 때만 수행한다. 이 문구가 없으면 PR 본문 초안만 작성하고 멈춘다.
13. 리뷰가 완료됐고 치명적 문제가 없으면 가능한 경우 `Final Approval` 상태로 전환한다. 해당 상태가 없거나 전환에 실패하면 `In Review`에 머물되 파일 수정, 상태 변경, PR 생성 없이 `Final Approval` 대기 상태임을 알린다.
14. 현재 상태가 `In Review`이고 `docs/ai-test-workflow/reports/review-report.md`가 이미 존재하면, 같은 리뷰를 반복하지 않는다. 기존 리뷰 결과를 요약하고 `Final Approval`이 필요하다고만 응답한다.

상태 전이:
- `Backlog`: 아직 착수하지 않은 이슈다.
- `Todo`: 작업 대기 상태다.
- `Research`: 저장소 근거를 조사하는 단계다.
- `Planning`: 계획을 정리했고 사람 승인을 기다리는 단계다. 이 상태에서는 구현을 시작하지 않는다.
- `Approved`: 사람이 승인했고 구현을 시작할 수 있는 단계다. 이 상태에서만 Implementer가 동작한다.
- `In Progress`: 승인된 범위 안에서 구현 중인 단계다.
- `In Review`: 구현이 끝났고 결과를 검토하는 단계다. 이 상태에서는 Reviewer만 검토를 수행한다.
- `Rework`: 검토 결과 수정이 필요한 단계다. 이 상태에서는 같은 범위만 다시 수정한다.
- `Final Approval`: 리뷰가 끝났고 최종 승인 대기 중인 단계다. 이 상태는 active state에 넣지 않는다.
- `Done`: 최종 수용이 끝난 상태다. agent는 이 상태로 직접 전환하지 않는다.

워크플로 메모:
- PR, 커밋, 리포트, 로그 같은 산출물은 상태가 아니라 작업 결과로 다룬다.
- Linear 상태는 작업의 진행 단계만 표현하고, 세부 산출물은 리포트/로그 문서로 남긴다.
- PR 본문이 필요하면 `.github/PULL_REQUEST_TEMPLATE.md`의 항목 순서와 제목을 그대로 따라 작성한다.
- PR 본문은 자유 서술로 대체하지 말고, 작업 개요 / 작업 내용 / PR 유형 / Check List / 관련 이슈 / 기타 참고 사항 항목을 빠짐없이 채운다.
- PR의 `관련 이슈`에는 GitHub 이슈 목록을 확인한 뒤 실제 이슈 번호를 `#123` 형식으로 적는다.
- 리뷰 결과 문제가 없으면 `docs/ai-test-workflow/reports/review-report.md`와 Linear 코멘트에 `Final Approval required`를 남기고 멈춘다.
- 사람이 최종 확인하기 전에는 workspace 정리를 유발할 수 있는 terminal state 전환을 수행하지 않는다.
