# Day 10 — Micro-model First

## Idea

Ticket classification usually goes straight to LLM, but most tickets contain obvious keywords.

**Level 1 — Micro-model** (local keyword-rule classifier):
- structured label (`billing|account|technical|feature_request`)
- confidence score (0..1)
- status `OK|UNSURE`
- ~0 ms latency, zero API cost

**Level 2 — LLM fallback** (`gpt-4o`) only when:
- status is `UNSURE`
- confidence < threshold (default 0.65)
- category format is invalid

## Run

```powershell
.\gradlew.bat runMicroModelEvaluation --console=plain
```

Env:
- `OPENROUTER_API_KEY` — required for fallback only
- `MICRO_FALLBACK_MODEL` — default `openai/gpt-4o`
- `MICRO_CONFIDENCE_THRESHOLD` — default `0.65`

Output: `micromodel/report.json` (30 test cases)

## Metrics

- micro handled vs fallback count
- total LLM calls (= fallback count)
- avg latency (micro + fallback)
- latency saved vs always-LLM estimate
- correctness split: micro vs fallback

## Architecture

```
MicromodelCli
  -> MicroFirstPipeline
       -> TicketKeywordMicroClassifier   (Level 1)
       -> MicroModelDecisionPolicy
       -> TicketClassificationGateway    (Level 2 fallback)
```

## Test matrix

| Kind | Count | Expected |
|---|---|---|
| SIMPLE | 15 | mostly micro |
| EDGE | 8 | mostly fallback |
| COMPLEX | 7 | mostly fallback |

Cases: `micromodel/test-cases.json`
