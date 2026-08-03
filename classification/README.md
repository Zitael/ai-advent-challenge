# Day 7 — Confidence-Controlled Inference

## Task

Support ticket classification where a wrong route is costly.
Inference runs **without fine-tuning**, with explicit confidence control before accepting a category.

## Approaches (4)

| Approach | Implementation |
|---|---|
| **Scoring** | Model returns JSON: category + confidence + status (`OK/UNSURE/FAIL`) |
| **Constraint-based** | Allowed labels, single-token format, non-empty content |
| **Redundancy** | 3 independent classifications, majority vote (2/3 required) |
| **Self-check** | Second prompt verifies or corrects the candidate category |

`ConfidenceOrchestrator` combines all checks. Result is accepted only when every gate passes and scoring status is `OK`. Otherwise the answer is rejected or marked `UNSURE/FAIL`. One full retry is attempted for non-OK first passes.

## Test matrix

| Kind | Purpose |
|---|---|
| `CORRECT` | Clear tickets with expected label |
| `EDGE` | Ambiguous multi-domain tickets |
| `NOISY` | Typos, noise, mixed keywords |

Cases live in `classification/test-cases.json`.

## Run

```powershell
.\gradlew.bat runConfidenceInference --console=plain
```

Requires `OPENROUTER_API_KEY`. Optional: `CONFIDENCE_MODEL` (default `openai/gpt-4o-mini`).

Output: `classification/report.json` with per-case outcomes and aggregate metrics:

- accepted / rejected / unsure / fail counts
- retry count
- baseline vs confidence latency, tokens, cost

## Architecture

```
ConfidenceInferenceCli
  -> ConfidenceEvaluationRunner
       -> TicketClassificationGateway (baseline, 1 call)
       -> ConfidenceOrchestrator
            -> ScoringClassifier
            -> ConstraintValidator
            -> RedundancyChecker
            -> SelfCheckVerifier
            -> ConfidenceDecisionEngine
```

Business logic stays in services; CLI only wires dependencies and prints summary.
