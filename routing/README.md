# Day 8 — Model Routing

## Strategy

1. **Cheap model first** (`gpt-4o-mini`) — scoring call with category + confidence + status
2. **Heuristics** — if any check fails → escalate to **strong model** (`gpt-4o`)
3. **Strong model fallback** — standard single-word classification

## Heuristics (3)

| Heuristic | Rule |
|---|---|
| `confidence_score` | Escalate if confidence < 0.8 |
| `answer_length` | Escalate if category token invalid length/format |
| `unsure_rule` | Escalate if status != `OK` |
| `constraint` | Escalate if category not in allowed set |
| `input_complexity` | Escalate on noisy text or multi-domain keywords |

## Run

```powershell
.\gradlew.bat runModelRouting --console=plain
```

Env:

- `OPENROUTER_API_KEY` — required
- `ROUTING_CHEAP_MODEL` — default `openai/gpt-4o-mini`
- `ROUTING_STRONG_MODEL` — default `openai/gpt-4o`

Output: `routing/report.json`

## Architecture

```
ModelRoutingCli
  -> RoutingEvaluationRunner
       -> ModelRouter
            -> ScoringClassifier (cheap model)
            -> RoutingHeuristics
            -> TicketClassificationGateway (strong model, fallback)
```

## Test matrix

| Kind | Expected routing |
|---|---|
| `SIMPLE` | Stay on cheap model |
| `EDGE` / `NOISY` | Escalate to strong model |

Cases: `routing/test-cases.json`
