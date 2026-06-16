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
| #148 | smoke checklist execution | test account, executor, health result, gateway admin/app smoke result, user and admin smoke results, rollback decision | task:ready + task:done |
| #149 | review pass evidence | reviewer, reviewed files, risk notes, linked review-ready or review-done issue state, release decision | task:ready + task:done |
| #150 | test pass evidence | command, result, timestamp, executor, distinction between build pass, test pass, and skipped tests | task:ready + task:done |
| #151 | CI build evidence | workflow run URL, release evidence file, commit SHA, immutable image tag, Maven/image/compose commands, health result, failure log path | task:ready + task:done |
| #152 | database execution evidence | backup path, backup SHA256, SQL commit SHA, executor, execution window, verification SQL summary, health result, rollback decision | task:ready + task:done |
| #153 | rollback version evidence | current SHA tag, current Git SHA, previous stable tag and SHA, compose/config/database boundaries, rollback command, verification command | task:ready + task:done |
| #154 | deployment health evidence | target environment, workflow run URL, release evidence file, compose deploy and ps output, health and gateway smoke results, rollback command | task:ready + task:done |

## Smoke evidence for #148

Use this template after smoke checks complete:

```text
aigc-community smoke evidence
- issue: #148
- test account:
- executor:
- executed at:
- target environment:
- commit sha:
- /actuator/health result:
- Gateway admin route result:
- Gateway app route result:
- User community list result:
- User detail result:
- User like result:
- User comment result:
- User share result:
- User follow result:
- Creator profile result:
- Admin list result:
- Admin audit approve result:
- Admin audit reject result:
- Admin hide/show result:
- Admin delete result:
- screenshots or logs:
- rollback decision:
- release decision:
```

If any smoke check fails, record the request, response, screenshot or log link, suspected owner, and rollback decision
before using `task:ready + task:done`.

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

## CI build evidence for #151

Use this template after the Gitea workflow or equivalent build proof completes:

```text
aigc-community CI build evidence
- issue: #151
- workflow run url:
- release evidence file:
- commit sha:
- immutable image tag:
- runner:
- docker version:
- docker compose version:
- maven command:
- image build command:
- compose up command:
- compose ps summary:
- service health result:
- failure logs path or "not required":
- release decision:
```

Do not mark #151 complete when the evidence cannot tie the build output to a Git SHA and immutable image tag.

## Database execution evidence for #152

Use this template after the database execution or dry-run evidence is captured:

```text
community_db migration record
- issue: #152
- environment:
- backup file:
- backup sha256:
- sql commit sha:
- executor:
- execution window:
- execute command:
- verification SQL summary:
- service health result:
- rollback owner:
- rollback decision:
- release decision:
```

The verification summary must cover `aigc_guide_content`, every `aigc_community_*` table, utf8mb4 collation, and the
indexes declared in `community_db.sql`.

## Rollback version evidence for #153

Use this template after the rollback target is identified:

```text
aigc-community rollback version record
- issue: #153
- current version tag:
- current git sha:
- previous stable tag:
- previous stable git sha:
- compose file:
- nacos config boundary:
- database rollback boundary:
- rollback command:
- verification command:
- release decision:
```

The current and previous stable tags must be immutable SHA tags for production evidence; `latest` is only acceptable as a
local-development fallback boundary.

## Deployment health evidence for #154

Use this template after deployment verification completes:

```text
aigc-community deployment health evidence
- issue: #154
- target environment:
- workflow run url:
- release evidence file:
- compose deploy command:
- compose ps summary:
- /actuator/health result:
- gateway admin smoke result:
- gateway app smoke result:
- key API smoke result:
- rollback command:
- rollback executed:
- release decision:
```

Do not mark #154 complete until the health response and gateway smoke results are real command output from the target
environment, or the release owner explicitly accepts the missing evidence and the issue is marked failed.

## Failure label rule

If a required evidence field cannot be filled from machine output or named reviewer/tester action, keep the issue on
`task:processing` and add `task:failed` with the missing evidence. Only use `task:ready + task:done` after the evidence
template has enough detail for an external release reviewer to reproduce or audit the decision.
