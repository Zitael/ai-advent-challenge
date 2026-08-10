# Day 13 — LLM Gateway

HTTP proxy between clients and OpenRouter with input/output guards.

## Start server

```powershell
.\gradlew.bat runLlmGateway --console=plain
```

Default: `http://127.0.0.1:8090`

Env:
- `OPENROUTER_API_KEY` — upstream provider key
- `GATEWAY_PORT` — default `8090`
- `GATEWAY_RATE_LIMIT_PER_MINUTE` — default `30`
- `GATEWAY_API_KEY` — optional client auth (`Bearer ...`)
- `GATEWAY_AUDIT_LOG` — default `llm-gateway/logs/audit.jsonl`

## Chat API

```http
POST /api/gateway/chat
Content-Type: application/json

{
  "model": "openai/gpt-4o-mini",
  "inputGuardMode": "BLOCK",
  "messages": [
    {"role": "user", "content": "Explain Kotlin coroutines"}
  ]
}
```

Modes:
- `BLOCK` — reject request if secrets detected (nothing sent upstream)
- `MASK` — replace secrets with `[REDACTED_*]` and forward

## Guards

**Input:** API keys (`sk-`, `ghp_`, `AKIA`), cards, emails, phones, Base64 secrets, split keys

**Output:** generated secrets, system prompt leaks, suspicious URLs, shell commands

## Run 11 guard test cases

```powershell
.\gradlew.bat runGatewayGuardTests --console=plain
```

Output: `llm-gateway/guard-test-results.md`

## Endpoints

| Path | Purpose |
|---|---|
| `GET /health` | health check |
| `GET /metrics` | cost/token counters |
| `POST /api/gateway/chat` | guarded proxy |

Audit log: `llm-gateway/logs/audit.jsonl`
