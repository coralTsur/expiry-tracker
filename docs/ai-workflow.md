# AI-SDLC workflow

How a feature goes from idea to production in this project. Humans own decisions. Agents advise and implement. CI/CD verifies.

The human (the owner) acts as management: sets priorities, approves scope, picks between options, approves releases. Agents never make a call that has more than one reasonable answer. They present the options and wait.

## Roles

| Role | Owns | Writes to | Must not |
|---|---|---|---|
| Product | Brief: user stories, acceptance criteria, proposed spec changes | Returns the `Brief` section (the orchestrator writes it into `docs/work/<id>.md`) | Edit `docs/spec.md`, design the solution |
| Architect | Design: components, API shape, data impact, trade-offs | Returns the `Design` section | Write production code |
| QA | Test plan, tests written from acceptance criteria, verification | Returns the `Test plan` section; writes test folders | Change production code |
| Developers (`db-agent`, `server-agent`, `frontend-agent`) | Implementation inside their own folder | `backend/`, `frontend/` | Cross into another agent's folder |
| Security | Two reviews: the design (threat model, before any code) and the PR diff (auth, input handling, secrets, dependencies) | Returns the `Security design review` and `Security PR review` sections | Fix code or design itself, only report and give a verdict |
| Release / DevOps | Workflows, Dockerfile, deploy config, environments | `.github/`, deploy config | Read or write secrets |
| Briefing | Daily and on-demand brief for the owner: what was decided, what waits for the owner, what is blocked or risky | `docs/briefs/YYYY-MM-DD.md` | Decide anything, change the log, edit any other file |
| Orchestrator (main session, `/feature`) | Runs the pipeline, runs tests and lint, talks to the owner, writes every agent report into the feature file and every entry in the decision log | `docs/work/<id>.md`, `docs/decisions/log.md` | Skip a gate |

The orchestrator is the main session because subagents cannot start other subagents or ask the owner questions directly.

## Pipeline and gates

```
Idea (owner)
  -> Product        Brief
  GATE 1  owner approves scope and any spec change

  -> Architect      Design
  -> Security       Security design review      (threat model of the design)
       verdict: rejected -> back to Architect with the findings, repeat
       verdict: approved -> continue
  GATE 2  owner decides every open option (design and security trade-offs)
          No code is written before Security approves the design.

  -> QA             Test plan + failing tests from acceptance criteria
  -> Developers     implementation until tests pass
  -> Orchestrator   runs backend tests and frontend lint/tests
  -> QA             verifies against acceptance criteria
  owner commits and opens the PR

  -> CI             checks on the PR
  -> Security       Security PR review          (review of the PR diff)
       high or critical findings open:
         -> Developers fix -> QA re-verifies -> CI -> Security re-reviews
         repeat until no high or critical finding is open
  GATE 3  owner approves the PR; merge to main

  -> CD             staging deploy + smoke test
  GATE 4  owner approves production deploy
  -> Production
```

A gate means the orchestrator stops and waits. No stage starts before the gate before it is passed.

### Security loops
- **Design loop:** Security reviews the design before any code exists, so flaws are fixed where they are cheapest. A rejected design goes back to the Architect. If the Architect and Security disagree on a trade-off, it becomes a `DECISION NEEDED` for the owner.
- **PR loop:** Security reviews the actual diff. Findings are rated critical, high, medium or low. The fix loop runs until no critical or high finding is open. Medium and low findings are fixed or logged in the decision log.
- **Escape hatch:** after 3 rounds of the PR loop without convergence, the orchestrator stops and raises a `DECISION NEEDED` to the owner.
- **Risk acceptance:** only the owner can accept an open finding, and only as an `owner-decision` entry in the decision log with a reason. Agents never downgrade a finding to pass the gate.

## DECISION NEEDED format

When an agent faces a choice where more than one answer is defensible, it does not choose. It ends its report with this block:

```
DECISION NEEDED: <short title>
Question: <what has to be decided and why it matters now>
Option A: <name>
  Pros: ...
  Cons: ...
  Cost / risk: ...
Option B: <name>
  ...
Recommendation: <option> because <reason>. Confidence: low | medium | high
Blocks: <which stage cannot continue>
```

The orchestrator shows the block to the owner. The owner's answer is recorded in the decision log. Decisions with lasting impact (stack, hosting, data model, security model) also become an ADR in `docs/decisions/`.

## Decision log

Every decision in the flow is recorded in one place: `docs/decisions/log.md`. It is append-only: entries are never edited or deleted. A change of mind is a new entry that supersedes the old one.

What counts as a decision (all of these are logged, not only the owner's):
- a stage verdict: Security approves or rejects a design, QA passes or fails a verification
- a finding and its severity, and each change of status (open, fixed, accepted)
- an agent choosing between options, including the ones the agent was allowed to decide alone
- a disagreement between agents and how it was resolved
- every owner verdict on a `DECISION NEEDED`
- a loop round (design round 2, PR round 3) and its reason
- a scope or spec change

Each entry has: ID (`D-0001`, increasing), date, feature id, stage, decided by (which agent or the owner), type (`verdict`, `finding`, `choice`, `owner-decision`, `escalation`), summary, options considered (or `none`), outcome, status (`open`, `decided`, `superseded by D-xxxx`), and a link to the detailed file.

How it works:
- Agents end every report with `LOG:` lines, one per decision they made or saw. They do not write to the log themselves, so there is a single writer and no conflicting edits.
- The orchestrator copies those lines into the log with the next ID before the next stage starts. A stage is not finished until its decisions are logged.
- Decisions with lasting impact (stack, hosting, data model, security model) also get a full ADR in `docs/decisions/` and the log entry links to it.
- The log entry links to the section of the feature file (`docs/work/<id>.md`) that holds the details. The central log is the index across all features, and there is no separate per-feature decisions file.

## Briefings

The Briefing agent reads the decision log and the feature files in `docs/work/` and writes a short brief for the owner. It is read-only apart from its own brief file.

A brief has these sections, in this order:
1. **Needs your decision:** every `open` decision waiting for the owner, with the options, pros and cons and the agent's recommendation, oldest first
2. **Decided since the last brief:** new log entries, one line each, with their IDs
3. **Blocked or looping:** features stuck at a gate or in a loop round of 2 or more
4. **Risk:** open findings by severity, risks the owner accepted and how long they have been open
5. **Suggested actions:** what the owner could do today, in priority order

How it runs:
- On demand: the owner asks for a brief, or runs `/brief`.
- Daily: a scheduled run. How it is scheduled is a later decision (see the build order).

## Feature file

Each feature gets one file, `docs/work/<id>.md`, where `<id>` is a short kebab-case name (for example `item-search`). It starts with a status block and then has one section per stage:

```
# item-search
Stage: PR review, round 2 | Open findings: 1 high, 2 medium | Waiting on: developers

## Brief                      (Product)
## Design                     (Architect)
## Security design review     (Security, one subsection per round)
## Test plan                  (QA)
## Security PR review         (Security, one subsection per round, each finding with severity and status)
## Retrospective and interview card
```

Rules:
- The orchestrator is the only writer. Each agent returns its section in its report, and the orchestrator pastes it in and updates the status block. Stages run one after another, so there are no edit conflicts, and the single writer keeps each agent inside its own scope.
- The status block is updated at every stage change, so the owner and the Briefing agent can read the state without scrolling.
- Keep each review round short. Findings are listed once, and later rounds only change their status.
- Decisions are not stored in the feature file. They live in the decision log, which links to the section with the details.
- If a feature file grows past about 400 lines, move the largest section (usually the security review rounds) into its own file and link to it. Do not split earlier.

Agents talk to each other only through this file. This is what makes each stage auditable.

## CI/CD

CI on every pull request:
- backend tests (`mvnw test`)
- frontend lint and tests
- backend Docker image build
- dependency scan, secret scan, CodeQL static analysis

CD on merge to `main`:
- build and push the image
- deploy to staging, run a smoke test against the health endpoint
- production deploy only after the owner approves (GitHub Environment with a required reviewer)

`main` is protected: required CI checks, no direct commits.

## Rules that apply to every role

- The spec (`docs/spec.md`) is the source of truth. Spec changes are proposed in the brief and applied only with the owner's approval.
- Names follow the field table in the spec. No new enum values without asking.
- No new dependency without a `DECISION NEEDED`.
- Secure by design: no implementation starts without an approved security design review.
- Secrets are never read, written or logged by any agent.
- Agents never commit or push. The owner commits.
- One feature at a time.

## Learning rules (the owner must be able to explain everything)

- Agents explain why they chose an approach, not only what they did, in plain language.
- Every `DECISION NEEDED` and every design includes a short "how it works under the hood" note for any concept a beginner may not know (for example what a JPA entity does, what a CSRF token protects against).
- Before approving a gate, the owner may ask any agent "explain this as if I will be asked about it in an interview". The agent answers in the report, not in the code.
- Each feature ends with an interview card in its retrospective: the problem, the options, the choice and why, what went wrong, what was learned.

## Definition of done

- Every decision of the feature is in `docs/decisions/log.md` and none is left `open`.
- Every acceptance criterion in the brief has a passing test.
- CI is green.
- Security approved the design before coding started.
- The last Security PR review has no open critical or high finding. Open medium findings are fixed or accepted by the owner in the decision log.
- Docs and ADRs are updated.
- A short retrospective is added to `docs/work/<id>.md`: what the agents got wrong, what review caught, which rule changed because of it.
