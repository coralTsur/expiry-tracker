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
