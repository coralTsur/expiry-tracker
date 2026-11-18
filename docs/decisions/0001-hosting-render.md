# 0001: Host on Render

- Status: accepted
- Date: 2026-11-14
- Decided by: owner (advised by the assistant)

## Context
The CD pipeline needs a deploy target for the Spring Boot backend (container) and the React frontend. The repo already has `backend/Dockerfile` and a `prod` profile that reads PostgreSQL (Neon) settings from environment variables.

## Options considered
### Option A: Render
Pros: runs the existing Dockerfile directly, free tier, staging and production environments, config as code (`render.yaml`), easy deploy triggers from GitHub Actions, works with Neon.
Cons: free tier cold starts, weaker enterprise resume signal than AWS or Azure.
Cost / risk: free to about $7/month.

### Option B: Fly.io
Pros: Docker-first, `fly.toml` as code, multi-region.
Cons: needs a card, more to learn, small free allowance.
Cost / risk: a few dollars per month.

### Option C: AWS (ECS or App Runner, S3 and CloudFront)
Pros: strongest resume signal, natural fit for Terraform, common in companies.
Cons: largest learning curve, IAM complexity, cost risk if misconfigured.
Cost / risk: about $10 to $30/month unless limited.

### Option D: Azure (Container Apps, Static Web Apps)
Pros: strong enterprise and Java signal, good GitHub Actions integration.
Cons: heavier setup than Render.
Cost / risk: low with free tiers.

## Decision
Render now. The owner may move to AWS later.

## Consequences
- Keep the deploy unit a Docker image and all configuration in environment variables, so a move to AWS means new deploy config, not code changes.
- Keep Render-specific files thin (`render.yaml` only).
- Revisit when the project needs infrastructure as code with Terraform or a stronger cloud signal for interviews.
