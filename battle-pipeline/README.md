# Battle Pipeline — Deploy & Red Team

Единый сервер для red-team сессии: веб-чат + LLM Gateway + guards.

## Быстрый старт (локально)

```powershell
# 1. Создай .env
copy .env.example .env   # или вручную

# 2. Заполни ключи
# OPENROUTER_API_KEY=sk-or-...
# BATTLE_API_KEY=your-long-random-key-min-20-chars

# 3. Запуск
.\gradlew.bat runBattlePipeline --console=plain
```

Открой: **http://127.0.0.1:8090/**

## VPS deploy

```bash
# На сервере
git clone <your-repo> && cd ai_advent_app
export OPENROUTER_API_KEY=sk-or-...
export BATTLE_API_KEY=$(openssl rand -hex 32)
export BATTLE_HOST=0.0.0.0
export BATTLE_PORT=8090

./gradlew runBattlePipeline
```

Рекомендуется nginx + TLS:

```nginx
server {
    listen 443 ssl;
    server_name battle.example.com;

    location / {
        proxy_pass http://127.0.0.1:8090;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header Host $host;
    }
}
```

Партнёру выдай:
- URL: `https://battle.example.com/`
- `BATTLE_API_KEY` (только ему, не в git)

## API

### `GET /`
Веб-чат (HTML).

### `GET /health`
```json
{
  "status": "ok",
  "service": "battle-pipeline",
  "model": "openai/gpt-4o-mini",
  "layers": ["prompt-injection-guard", "indirect-content-sanitizer", ...]
}
```

### `POST /api/chat`
**Auth:** `Authorization: Bearer <BATTLE_API_KEY>`

**Request:**
```json
{
  "sessionId": "partner-session-1",
  "message": "Привет"
}
```

**Response:**
```json
{
  "sessionId": "partner-session-1",
  "answer": "...",
  "model": "openai/gpt-4o-mini",
  "durationMillis": 1200,
  "historyMessages": 2,
  "guards": {
    "injectionBlocked": false,
    "injectionPatterns": [],
    "indirectArtifactsRemoved": [],
    "gatewayInputFindings": [],
    "gatewayOutputViolations": [],
    "outputBlocked": false
  }
}
```

При блокировке injection/gateway — `answer` содержит refusal message, guards показывают что сработало.

### `POST /api/gateway/chat`
Прямой доступ к gateway (для тестов execution loop). Тот же Bearer key.

### `GET /metrics`
Token/cost counters.

### `DELETE /api/sessions/{sessionId}`
Очистить историю чата.

### Workspace files

| Method | Path | Description |
|---|---|---|
| GET | `/api/files` | список файлов |
| GET | `/api/files/{name}` | содержимое |
| PUT | `/api/files/{name}` | сохранить `{"content":"..."}` |
| DELETE | `/api/files/{name}` | удалить (seed защищён) |

Seed: `internal-secrets.env` — demo-секреты. Агент видит файл, но не должен выдавать значения (`BattleSecretLeakGuard`).

Env: `BATTLE_WORKSPACE_DIR` → `battle-pipeline/workspace`

## Слои защиты

1. **PromptInjectionGuard** — jailbreak, forget instructions, security bypass
2. **IndirectContentSanitizer** — HTML comments, zero-width, hidden links
3. **Gateway InputGuard** — secrets (sk-, ghp_, AKIA), base64, split keys, comments scan
4. **Gateway OutputGuard** — leaked secrets, system prompt, suspicious URLs
5. **Hardened system prompt** — workspace + user boundaries
6. **BattleSecretLeakGuard** — blocks quoted secrets from workspace files
7. **Execution loop security step** — heuristic + LLM review before commit

## Red team baseline

```powershell
.\gradlew.bat runBattleRedTeam --console=plain
```

Отчёт: `battle-pipeline/red-team-baseline.md`

## Проверка перед боем

```powershell
.\gradlew.bat test runBattleRedTeam runGatewayGuardTests runSecurityProbe --console=plain
```

## Env vars

| Variable | Required | Default |
|---|---|---|
| `OPENROUTER_API_KEY` | yes | — |
| `BATTLE_API_KEY` | yes | — |
| `BATTLE_HOST` | no | `0.0.0.0` |
| `BATTLE_PORT` | no | `8090` |
| `GATEWAY_DEFAULT_MODEL` | no | `openai/gpt-4o-mini` |
| `GATEWAY_RATE_LIMIT_PER_MINUTE` | no | `30` |
| `GATEWAY_AUDIT_LOG` | no | `llm-gateway/logs/audit.jsonl` |
| `BATTLE_WORKSPACE_DIR` | no | `battle-pipeline/workspace` |

## Red team report template

См. `battle-pipeline/red-team-report-template.md` — партнёр заполняет после атаки, ты фиксишь и прогоняешь повторно.
