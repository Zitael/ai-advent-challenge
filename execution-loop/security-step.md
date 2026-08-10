# Day 14 — Execution Loop + Security Step

Extends the Week 2 execution loop with a security review gate before commit.
All LLM calls (generation + security review) go through the Day 13 LLM Gateway.

## Flow

```
prompt → gateway → generate code → validate (lint/tests)
  → security review (gateway + heuristics + LLM)
  → Critical/High → retry generation with feedback
  → Medium/Low → commit with warning
  → clean → commit
```

## Prerequisites

Start the gateway:

```powershell
.\gradlew.bat runLlmGateway --console=plain
```

Env: `OPENROUTER_API_KEY`, optional `GATEWAY_API_KEY`.

## Run execution loop with security step

```powershell
.\gradlew.bat runExecutionLoop --console=plain -PexecutionLoopQueue=execution-loop/security-probe-tasks.md -PexecutionLoopLimit=3 --no-commit
```

Flags:
- `--no-commit` — skip git commits
- `--no-security` — disable security review step
- `--queue=path/to/tasks.md`
- `--limit=N`

Env:
- `GATEWAY_BASE_URL` — default `http://127.0.0.1:8090`
- `GATEWAY_DEFAULT_MODEL` — default `openai/gpt-4o-mini`

## Security probe (offline, no live LLM)

Runs 3 intentionally unsafe scenarios with simulated agent output:

```powershell
.\gradlew.bat runSecurityProbe --console=plain
```

Output: `execution-loop/security-probe/security-probe-results.md`

## Security prompt scope (Kotlin/Ktor)

- Hardcoded secrets (`sk-`, `ghp_`, `AKIA`, passwords)
- PII / auth tokens in logs
- HTTP instead of HTTPS
- Missing input validation
- SQL injection via string interpolation
- Insecure token storage (plain files, properties)

## Logs

- Execution run: `build/execution-loop/logs/<run-id>.md`
- Gateway audit: `llm-gateway/logs/audit.jsonl`
- Security probe: `execution-loop/security-probe/security-probe-results.md`
