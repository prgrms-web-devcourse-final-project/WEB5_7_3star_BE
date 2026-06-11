# AI Assisted Test Workflow

TrainUs 테스트 품질 개선 작업에서 AI 에이전트를 안전하게 활용하기 위한 문서 모음이다.

목표는 AI에게 바로 구현을 맡기는 것이 아니라, 조사, 계획, 검토, 승인, 실행 기록을 분리해 테스트 보강 과정을 추적 가능하게 만드는 것이다.

## Directory

```text
docs/ai-test-workflow/
  rules/       에이전트 역할, 승인 정책, 테스트 경계, 강제 체크리스트
  prompts/     에이전트별 실행 프롬프트
  templates/   조사/계획/검토 산출물 양식
  logs/        프롬프트와 도구 사용 기록
```

## Usage

1. `rules/` 문서를 먼저 읽고 작업 경계를 확인한다.
2. `prompts/agent-prompts.md`에서 역할별 프롬프트를 사용한다.
3. 에이전트 결과는 `templates/` 양식에 맞춰 정리한다.
4. 실제 실행 프롬프트, 명령, 결과는 `logs/prompt-and-tool-log.md`에 남긴다.

