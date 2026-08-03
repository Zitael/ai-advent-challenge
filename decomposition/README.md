# Day 9 — Inference Decomposition

## Task

Support ticket triage with **multiple fields**:

- `category` — billing / account / technical / feature_request
- `priority` — low / medium / high / urgent
- `action` — auto_reply / escalate / human_review
- `summary` — short CRM note

One monolithic prompt handles this poorly on noisy / multi-domain tickets.

## Variants

### A — Monolithic

One LLM call → one TOON line:

`category=...|priority=...|action=...|summary=...`

### B — Multi-stage

| Stage | Role | Model (default) | TOON format |
|---|---|---|---|
| 1 analyze | normalize input, extract signals | `gpt-4o-mini` | `intent=...|signals=...|clean=...` |
| 2 decide | classify + policy decision | `gpt-4o` | `category=...|priority=...|action=...` |
| 3 format | final CRM record | `gpt-4o-mini` | `category=...|priority=...|action=...|summary=...` |

Each stage: short prompt, strict compact TOON, low temperature.

## Run

```powershell
.\gradlew.bat runDecompositionEvaluation --console=plain
```

Env:

- `OPENROUTER_API_KEY` — required
- `DECOMP_MONOLITHIC_MODEL` — default `openai/gpt-4o-mini`
- `DECOMP_ANALYZE_MODEL` — default `openai/gpt-4o-mini`
- `DECOMP_DECIDE_MODEL` — default `openai/gpt-4o`
- `DECOMP_FORMAT_MODEL` — default `openai/gpt-4o-mini`

Output: `decomposition/report.json`

## Metrics

- format validity (strict TOON parse)
- matched expected fields (category / priority / action)
- latency, tokens, cost — monolithic vs multi-stage

## Architecture

```
DecompositionCli
  -> DecompositionEvaluationRunner
       -> MonolithicTriageService (variant A)
       -> MultiStageTriagePipeline (variant B)
            -> StageExecutor (per-stage model)
            -> CompactToonParser
```
