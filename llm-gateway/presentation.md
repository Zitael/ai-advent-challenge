# Day 13 — Сценарий для видео (5–6 мин)

## 1. Зачем gateway (45 сек)

Клиенты не должны ходить в OpenAI напрямую:
- секреты в промптах
- audit trail
- rate limits / cost control

## 2. Архитектура (1 мин)

```
Client -> Ktor Gateway -> Input Guard -> OpenRouter -> Output Guard -> Client
                |                              |
           audit.jsonl                   cost metrics
```

## 3. Input Guard demo (1.5 мин)

```powershell
.\gradlew.bat runGatewayGuardTests --console=plain
```

Показать `guard-test-results.md` — 11 кейсов:
- clean → ALLOW
- sk-/ghp_/AKIA → BLOCK
- mask mode → `[REDACTED_API_KEY]`
- split key, Base64

## 4. Live server (1.5 мин)

```powershell
.\gradlew.bat runLlmGateway
```

```powershell
curl -X POST http://127.0.0.1:8090/api/gateway/chat `
  -H "Content-Type: application/json" `
  -d '{"messages":[{"role":"user","content":"My key sk-proj-abc123XYZ789012345678"}],"inputGuardMode":"BLOCK"}'
```

→ `input_guard_blocked`, ничего не ушло в LLM.

Затем clean prompt → normal answer.

Показать `llm-gateway/logs/audit.jsonl`.

## 5. Output Guard + metrics (45 сек)

- Output с `SYSTEM PROMPT:` → blocked
- `GET /metrics` — tokens + cost

## 6. Takeaway (30 сек)

Gateway = единая точка для policy enforcement. Guards не идеальны (split/obfuscation), но закрывают 80% leak cases.
