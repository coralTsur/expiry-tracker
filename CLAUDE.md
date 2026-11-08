# expiry-tracker

Full-stack app for tracking things that expire: gift cards, insurance, rent, car tests.
The spec is the source of truth: `docs/spec.md`. Read it before any task that touches data, API or screens.

## Stack
- Backend: Java 21, Spring Boot, Maven (via the Maven wrapper `mvnw`)
- Frontend: React + Vite, JavaScript (no TypeScript), React Router (declarative mode)
- Database: H2 to start, PostgreSQL later
- Dev machine: Windows, PowerShell, VS Code

## Project layout
- `docs/`      spec, API definition, decisions
- `backend/`   Spring Boot app, including the data layer (entities, repositories, schema)
- `frontend/`  React app
- Backend tests live in `backend/src/test/java` (Maven standard). Frontend tests live next to the frontend code.

## Commands (PowerShell)
Run backend commands from the `backend/` folder:
- Backend test:  `.\mvnw.cmd test`
- Backend run:   `.\mvnw.cmd spring-boot:run` (serves on http://localhost:8080, stop with Ctrl+C)

Run frontend commands from the `frontend/` folder. Use `npm.cmd`, not `npm` (PowerShell may block `npm.ps1`):
- Frontend install: `npm.cmd install` (after cloning, or when `package.json` changes)
- Frontend run:  `npm.cmd run dev` (serves on http://localhost:5173, stop with Ctrl+C). The backend must be running on port 8080 for data to load.
- Frontend lint:  `npm.cmd run lint`

- Use PowerShell syntax only. No bash-only commands (no `&&` chains, no `rm -rf`, no `export`).

## Naming conventions
- Field and variable names: camelCase, exactly as in the field table in `docs/spec.md`
- Booleans have no `is` prefix: `redeemed`, `reminderEnabled`
- Category enum: `GIFT_CARD`, `INSURANCE`, `RENT`, `CAR_TEST`, `OTHER`
- Do not rename spec fields or add enum values without asking me first

## Working rules
- One task at a time. When a task is done, stop and wait for my review.
- Before writing code for a task, say in a few lines what you plan to change and in which files.
- Work only inside the folder that belongs to the task. Do not touch other folders.
- Explain why you chose an approach, not just what you did. I need to be able to explain every part of this code in an interview.
- Prefer simple, readable code over clever code. Do not add libraries or dependencies without asking.
- If the spec is unclear or missing something, ask me. Do not invent requirements, and do not edit `docs/spec.md` without my approval.
- Never read, open or edit `.env` or any file with secrets.

## Git
- Never commit or push without my explicit approval. Show me the list of changed files and the commit message first.
- Stage files by name (`git add path/to/file`). Never use `git add .` or `git add -A`.
- One branch per stage. Do not commit to `main` directly.
- Commit messages in English, short and in the imperative ("Add item entity").
