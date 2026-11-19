# Decision log

Append-only. Never edit or delete an entry: add a new one that supersedes it. Rules are in `docs/ai-workflow.md`.

Types: `verdict`, `finding`, `choice`, `owner-decision`, `escalation`.
Status: `open`, `decided`, `superseded by D-xxxx`.

| ID | Date | Feature | Stage | Decided by | Type | Summary | Options | Outcome | Status | Details |
|---|---|---|---|---|---|---|---|---|---|---|
| D-0001 | 2026-11-14 | platform | infra | owner | owner-decision | Host on Render, AWS possible later | Render, Fly.io, AWS, Azure | Render | decided | [ADR 0001](0001-hosting-render.md) |
| D-0002 | 2026-11-14 | platform | process | owner | owner-decision | Security reviews the design before any code, and the PR diff in a fix loop until no critical or high finding is open | none | Adopted in the workflow doc | decided | [ai-workflow.md](../ai-workflow.md) |
| D-0003 | 2026-11-14 | platform | process | owner | owner-decision | Every decision in the flow is logged centrally, and a Briefing agent reports to the owner daily and on demand | none | Adopted in the workflow doc | decided | [ai-workflow.md](../ai-workflow.md) |
| D-0004 | 2026-11-15 | platform | process | owner | owner-decision | One file per feature (`docs/work/<id>.md`) with a section per stage and a status block, instead of six files; no per-feature decisions file | Six files per feature, one file per feature | One file, split a section out only past about 400 lines | decided | [ai-workflow.md](../ai-workflow.md) |
| D-0005 | 2026-11-18 | platform | ci-security | owner | owner-decision | Pin GitHub Actions to commit hashes of the latest v4.x releases (the tested line), not the newest majors | latest v4.x, newest majors | Latest v4.x; Dependabot proposes upgrades later | decided | [ci.yml](../../.github/workflows/ci.yml) |
| D-0006 | 2026-11-18 | platform | ci-security | owner | owner-decision | Dependency scan: Dependabot plus dependency-review | Dependabot + dependency-review, npm audit + OWASP dependency-check | Dependabot + dependency-review | decided | [dependabot.yml](../../.github/dependabot.yml) |
| D-0007 | 2026-11-18 | platform | ci-security | owner | owner-decision | Secret scan: GitHub secret scanning with push protection (repo setting) | GitHub secret scanning, gitleaks in CI | GitHub secret scanning + push protection | decided | [ai-workflow.md](../ai-workflow.md) |
| D-0008 | 2026-11-19 | platform | ci-security | owner | owner-decision | Group Dependabot minor and patch updates into one PR per ecosystem; major updates stay separate PRs | one PR per dependency, grouped minor+patch | Grouped minor+patch per ecosystem | decided | [dependabot.yml](../../.github/dependabot.yml) |
| D-0009 | 2026-11-20 | platform | process | owner | owner-decision | Three change tiers (small, standard, full): the orchestrator proposes a tier with reasons and the owner confirms; a change touching schema, API contract, auth, secrets or config, dependencies, workflows, infra or personal data is always tier 3 | 3 tiers with owner confirmation, always the full flow, orchestrator decides alone | 3 tiers, orchestrator proposes, owner confirms | decided | [ai-workflow.md](../ai-workflow.md) |
