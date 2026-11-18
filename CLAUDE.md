# rest-api-3

학습용 REST API 프로젝트. 결과물만큼 **과정을 나중에 다시 따라 읽을 수 있는 것**이 중요하다.
문서·코드 코멘트·커밋 메시지는 한국어로 쓴다 (식별자와 기술 용어는 영어 그대로).

## 작업 흐름

기능은 아래 순서로 진행한다. 단계를 건너뛰지 않는다.

1. `/grill-with-docs`: 계획을 질문으로 다듬고, 확정된 용어는 `GLOSSARY.md`, 결정은 `docs/adr/`에 남긴다.
2. `/to-spec`: 합의된 내용을 GitHub 이슈 spec으로 만든다.
3. `/to-tickets`: spec을 구현 가능한 ticket 이슈들로 쪼갠다.
4. `/implement-spec`: ticket 단위로 구현하고 커밋한다.

## 코드 코멘트: 학습용으로 자세히

- 각 파일 상단에 이 파일의 역할과, 요청 흐름에서 어디에 위치하는지 설명한다.
- "무엇"보다 **"왜"**를 쓴다: 왜 이 구조/라이브러리/상태 코드를 골랐는지, 다른 선택지는 무엇이었는지.
- 처음 등장하는 개념(미들웨어, DTO, 트랜잭션, HTTP 상태 코드, 검증 등)은 처음 나오는 곳에서 짧게 풀어 설명한다.
- 흔한 실수나 함정이 있는 곳에는 `// 주의:`로 표시한다.

## 커밋: 메시지는 자세히, 커밋마다 번호 태그

**메시지 형식**

```
<태그> <type>: <한 줄 요약>

## 무엇을 했나
- 변경 사항 목록

## 왜
- 이렇게 한 이유, 고려한 대안

## 배운 점 / 핵심 개념
- 이 단계에서 익힌 개념

Refs #<ticket 번호>
```

`type`은 `feat`, `fix`, `refactor`, `test`, `docs`, `chore` 중 하나. 제목 맨 앞에는 이 커밋에 달 태그 번호를 붙인다. 예: `005 feat: 글 수정 PUT /api/v1/posts/{id}`

**태그 규칙**

- 커밋마다 3자리 일련번호로 annotated tag를 단다: `000`(초기 세팅), `001`, `002`, … (번호는 건너뛰지 않는다)
- 다음 번호는 `git tag --list '[0-9][0-9][0-9]' | sort | tail -1`에 1을 더해서 정한다.
- 태그 메시지는 커밋 요약 한 줄: `git tag -a 004 -m "004: <요약>"`
- 커밋과 태그를 함께 push한다: `git push origin HEAD && git push origin <태그>`
- 그러면 GitHub에서 `https://github.com/zoek1211/rest-api-3/tree/<태그>`(그 시점 전체 코드)와 `https://github.com/zoek1211/rest-api-3/commit/<태그>`(그 단계에서 바뀐 코드) 주소로 바로 볼 수 있다.
- README 단계 제목의 "코드 보기" 링크는 `/commit/<태그>`를 쓴다. 목차 표의 링크는 `/tree/<태그>`를 쓴다.
- `/implement-spec`이 서브에이전트를 병렬로 돌려도 태그는 **통합 브랜치에 들어간 순서대로** 메인 에이전트가 단다. 그래야 번호가 겹치거나 빠지지 않는다. 같은 커밋에 태그를 두 번 달지 않는다.

## README: 단계별 학습 노트

태그를 달 때마다 `README.md`의 "단계별 진행" 섹션에 항목을 하나 추가하고, 같은 커밋에 넣는다.

````markdown
### 004: <요약>  ·  [코드 보기](https://github.com/zoek1211/rest-api-3/commit/004)

**무엇을 했나**: 2–3문장
**왜**: 핵심 이유
**핵심 코드**

```ts
// 이 단계의 핵심이 되는 짧은 코드 발췌 (전체 파일 X, 10–25줄)
```

**배운 점**: 개념 1–3개
````

README 맨 위에는 프로젝트 소개, 실행 방법, 그리고 전체 단계 목차(태그 링크)를 둔다.

## Agent skills

### Issue tracker

Issues are tracked in GitHub Issues for `zoek1211/rest-api-3` via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root, created lazily. See `docs/agents/domain.md`.
