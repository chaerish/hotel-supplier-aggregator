---
name: git-workflow
description: Use this skill whenever committing changes, opening a GitHub issue, branching off an issue, or opening a PR in this repository. Also use before any `git commit` to check work-unit separation and message format. Triggers on: "커밋해줘", "커밋 만들어줘", "이슈 만들어줘", "브랜치 파줘", "PR 올려줘", "오늘 할 일 이슈로 쪼개줘".
---

# Git 작업 흐름 (이 저장소 전용)

## 0. 계정 규칙 — 가장 먼저, 항상 지킨다
- **모든 커밋은 사용자 본인 git 계정으로만 남는다.** (`git config user.name` / `user.email`에 설정된 계정 그대로)
- **커밋 메시지에 AI/Claude 관련 문구를 절대 포함하지 않는다.** `Co-Authored-By: Claude ...` 트레일러, "AI가 작성/제안" 같은 언급 모두 금지.
  - Claude Code의 기본 동작은 커밋 시 `Co-Authored-By: Claude ...` 트레일러를 자동으로 붙이지만, **이 저장소에서는 사용자가 명시적으로 이를 금지했으므로 그 기본 동작을 따르지 않는다.**
- 커밋을 만들기 직전, 최종 메시지 문자열에 `Claude`, `Co-Authored-By`, `Anthropic` 같은 단어가 없는지 마지막으로 확인한다.

## 1. 하루 작업 흐름: 할 일 → 이슈 → 브랜치 → PR
1. 오늘 할 일을 기능/작업 단위로 쪼갠다.
2. 각 단위를 GitHub 이슈로 만든다 — `gh issue create --title "..." --body "..."`
3. 이슈 기반으로 브랜치를 판다 — 이 저장소 기존 브랜치 네이밍(`feature/supplier-adapter`)을 따라 `<type>/<설명>` 형태를 쓴다 (`feature/`, `fix/`, `docs/` 등). 이슈 번호를 붙이고 싶으면 `feature/12-mock-port-split`처럼 접두에 붙인다.
4. 작업 후 PR을 올린다 — `gh pr create`, 본문에 `Closes #<이슈번호>`로 이슈와 연결한다.

## 2. 커밋 단위 분리 — 섞지 않는다
- **서로 다른 작업 단위를 한 커밋에 섞지 않는다.** (예: "Mock 포트 분리"와 "PostgreSQL 전환"은 각각 별도 커밋)
- **같은 파일 안에 서로 다른 작업 내용이 섞여 있어도 나눠서 커밋한다.** `git add -p`(patch 모드)로 관련된 hunk만 골라 스테이지하고, 나머지는 다음 커밋으로 남긴다.
- 커밋 전 `git status` / `git diff --staged`로 실제로 무엇이 올라가는지 반드시 확인한다. 의도치 않은 파일이 섞여 있지 않은지도 함께 확인한다.

## 3. 커밋 메시지 형식
- **제목(첫 줄)**: 한국어. `<type>: <한국어 제목>` 형식을 쓴다. `type`은 변경 성격에 맞춰 고른다 (`feat`, `fix`, `chore`, `docs`, `refactor`, `test` 등).
- **본문**: 한국어. 무엇을/왜 했는지 **3문장 이내**로 서술.
- 본문에 세부 작업 내용을 **개조식**(명사형 종결, 완전한 문장 아님) bullet list로 나열한다.
- `Co-Authored-By` 등 AI 관련 트레일러는 절대 넣지 않는다 (0번 규칙).

### 템플릿
```
<type>: <한국어 제목>

<한국어 본문, 3문장 이내로 무엇을/왜>

- <세부 작업 1, 개조식>
- <세부 작업 2, 개조식>변경 성격에 맞는가
- [ ] 본문이 3문장 이내 서술 + 개조식 bullet list로 구성됐는가
- [ ] `Co-Authored-By`/`Claude`/`Anthropic` 같은 AI 관련 트레일러가 없는가
- [ ] 커밋자 계정이 본인 계정인지 확인했는가 (`git config user.email`)
- [ ] `CLAUDE.local.md`에 적힌 점검 항목도 함께 확인했는가

## 5. PR 규칙
- **제목**: 커밋 메시지의 제목을 그대로 쓴다 (`type: 한국어 제목`). 브랜치에 커밋이 여러 개면, 그 브랜치의 작업을 가장 잘 대표하는 커밋 제목을 고른다.
- **본문**: `.github/pull_request_template.md`를 그대로 따른다.
  - `## 개요`: 무엇을 왜 바꿨는지 간단히 서술
  - `Resolves: #<이슈번호>`로 이슈와 연결
  - `## PR 유형`: 해당하는 항목에 체크
  - `## PR Checklist`: 커밋 메시지 컨벤션 준수, 테스트 여부를 실제로 확인하고 체크
- `gh pr create --title "..." --body "$(cat <<'EOF' ... EOF)"` 형태로, 템플릿 형식을 유지한 본문을 직접 채워서 연다.
