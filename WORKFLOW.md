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
  max_turns: 4
codex:
  command: codex --model gpt-5.4-mini --config model_reasoning_effort="high" --config shell_environment_policy.inherit=all app-server
  approval_policy: never
  thread_sandbox: workspace-write
  turn_sandbox_policy:
    type: workspaceWrite
    networkAccess: true
---

You are working on Linear issue `{{ issue.identifier }}`.

## Basics

- This file contains only execution settings and shared rules.
- Follow `docs/ai-test-workflow/rules/*.md` and the issue body for role details and test boundaries.
- Do not guess when evidence is missing. Record only missing evidence.
- Write user-facing output in Korean.
- Keep code identifiers, file paths, class names, method names, commands, and exception names unchanged.

## Artifacts

- Researcher: `docs/ai-test-workflow/reports/{{ issue.identifier }}/research-report.md`
- Planner: `docs/ai-test-workflow/reports/{{ issue.identifier }}/test-plan.md`
- Reviewer: `docs/ai-test-workflow/reports/{{ issue.identifier }}/review-report.md`
- PR draft: `docs/ai-test-workflow/reports/{{ issue.identifier }}/pr-draft.md`
- Log: `docs/ai-test-workflow/logs/{{ issue.identifier }}/prompt-and-tool-log.md`

## Issue Info

- Identifier: `{{ issue.identifier }}`
- Title: `{{ issue.title }}`
- State: `{{ issue.state }}`
- Labels: `{{ issue.labels }}`
- URL: `{{ issue.url }}`

{% if issue.description %}
{{ issue.description }}
{% else %}
No description provided.
{% endif %}

## First Run

- If `.codegraph/` is missing, run `codegraph init -i` once.
- Reuse the index if it already exists.
- Prefer CodeGraph for structural queries.

## Rules

- Use the issue as the single source of truth.
- Stay within issue scope.
- Do not modify production code before approval.
- After planning, move to `Planning` and stop.
- `Planning` is not an active state and must not keep retrying.
- If the current state is `Approved`, do not rewrite the plan; move to implementation.
- If `test-plan.md` already exists and the approval scope is unchanged, do not regenerate the plan.
- After implementation, move to `In Review`.
- After review, move to `Final Approval` and stop.
- `Final Approval` is not an active state and must not keep retrying.
- Do not move directly to `Done`, `Closed`, `Cancelled`, `Canceled`, or `Duplicate`.
- Do not `git push`, create PRs, or merge PRs.
- Keep PR text as draft only.
- PR drafts must follow `.github/PULL_REQUEST_TEMPLATE.md`.
- Related issue references in PR drafts must use `#123` format.
- Use `Final Approval` only for human confirmation; do not include it in active states.
- Decide whether to repeat review based on the current issue, scope, branch, and latest commit, not on file existence alone.
- If the same snapshot was already reviewed, keep only a summary and move to `Final Approval`.

## States

- `Backlog`: not started
- `Todo`: waiting
- `Research`: investigation
- `Planning`: plan ready, waiting for approval
- `Approved`: approved
- `In Progress`: implementing
- `In Review`: reviewing
- `Rework`: fixing
- `Final Approval`: final approval waiting
- `Done`: fully complete

## Minimal Record

- Reports keep only purpose, evidence, findings, limits, and next steps.
- Logs keep prompts, tools, model name, and reasoning effort.
- Record models with concrete values such as `gpt-5.4-mini` and `model_reasoning_effort=high`.
- Logs also keep per-stage token usage.
- Example stages: `Research`, `Planning`, `Implementation`, `Review`, `Approval`.
- Store artifacts under issue-specific directories.
- Do not overwrite artifacts from previous issues.
- Final response must summarize completed work, modified files, validation, and remaining blockers only.
