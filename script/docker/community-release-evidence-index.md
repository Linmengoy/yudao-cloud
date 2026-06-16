# aigc-community release evidence index

This index maps the release-gate issues to concrete evidence that can be pasted back into Gitea after each run.
It is intentionally linked from the CI workflow and test suite so release readiness does not depend on memory or
ad-hoc chat summaries.

## Issue evidence map

| Issue | Gate | Required evidence | Passing label |
| --- | --- | --- | --- |
| #145 | auditable CI/CD pipeline | workflow run URL, commit SHA, runner, Docker versions, Maven command, image build command, compose up command, compose ps summary, failure logs when present | task:ready + task:done |
| #146 | immutable image version | current SHA tag, previous stable tag, image list, rollback command, local `latest` fallback boundary | task:ready + task:done |
| #147 | database migration record | backup path, backup SHA256, SQL commit SHA, execution window, verification SQL summary, rollback owner and decision | task:ready + task:done |
| #149 | review pass evidence | reviewer, reviewed files, risk notes, linked review-ready or review-done issue state, release decision | task:ready + task:done |
| #150 | test pass evidence | command, result, timestamp, executor, distinction between build pass, test pass, and skipped tests | task:ready + task:done |

## Review evidence for #149

Use this template after review completes:

```text
aigc-community review evidence
- issue: #149
- reviewer:
- reviewed at:
- commit sha:
- reviewed files:
- review scope: code, config, SQL, runbook, workflow
- linked gates: #145, #146, #147, #150
- blocking findings: none | <details>
- release decision: review:ready | review:done | blocked
```

Do not mark #149 complete when any linked release gate lacks a reviewer, reviewed files, or a release decision.

## Test evidence for #150

Use this template after tests complete:

```text
aigc-community test evidence
- issue: #150
- executor:
- executed at:
- commit sha:
- build command: mvn clean package -pl yudao-module-aigc-community/yudao-module-aigc-community-server -am -DskipTests
- contract test command: python -m pytest tests/test_community_release_gates.py tests/test_review_ready_contracts.py
- smoke test command: curl -fsS http://127.0.0.1:48097/actuator/health
- build result: passed | failed | skipped
- test result: passed | failed | skipped
- skipped tests: none | <reason>
- release decision: test:done | blocked
```

Do not use `test:done` when the only successful command is a Maven build with `-DskipTests`.

## Failure label rule

If a required evidence field cannot be filled from machine output or named reviewer/tester action, keep the issue on
`task:processing` and add `task:failed` with the missing evidence. Only use `task:ready + task:done` after the evidence
template has enough detail for an external release reviewer to reproduce or audit the decision.
