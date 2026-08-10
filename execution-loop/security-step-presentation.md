# Day 14 — Сценарий для видео (5–6 мин)

## 1. Execution loop + security gate (45 сек)

Week 2 loop: prompt → generate → validate → **security review** → commit.

Critical/High → retry с фидбеком. Medium/Low → warning. Clean → commit.

## 2. Все вызовы через Gateway (1 мин)

```powershell
.\gradlew.bat runLlmGateway
.\gradlew.bat runExecutionLoop -PexecutionLoopQueue=execution-loop/security-probe-tasks.md --no-commit
```

- Generation и security review → `POST /api/gateway/chat`
- Context sanitizer маскирует секреты из codebase до отправки
- Audit: `llm-gateway/logs/audit.jsonl`

## 3. Security probe offline (1.5 мин)

```powershell
.\gradlew.bat runSecurityProbe --console=plain
```

Показать `security-probe-results.md`:
- sec-probe-001: token storage → heuristic + gateway
- sec-probe-002: request logging → PII in logs
- sec-probe-003: HTTP API без validation

## 4. Security prompt (Kotlin/Ktor) (1 мин)

Проверяем: hardcoded secrets, PII в логах, HTTP, SQL injection, insecure token storage.

## 5. Что поймали / что пропустили (1 мин)

| Слой | sec-probe-001 | sec-probe-002 | sec-probe-003 |
|---|---|---|---|
| Heuristic | token file, sk- key | println+Authorization | http:// |
| Gateway | sk- in raw prompt | — | — |
| Security step | BLOCK | BLOCK | BLOCK |

## 6. Takeaway (30 сек)

Defense in depth: gateway защищает промпты, security step — код перед коммитом. Ни один слой не заменяет другой.
