# Evaluation Criteria — Day 6 Ticket Classification

## Task

Fine-tune a model to classify support tickets into one of four categories:

- `billing`
- `account`
- `technical`
- `feature_request`

## Baseline model

- Provider: OpenRouter
- Model: `openai/gpt-4o-mini`
- Fine-tuning: **not applied** (baseline only)

## How we decide "became better"

| Criterion | Baseline expectation | Target after fine-tune |
|---|---|---|
| **Accuracy** | Exact match of category word on eval set | ≥ 90% exact match |
| **Format compliance** | Model may add explanations or wrong casing | 100% single lowercase token from allowed set |
| **Style** | Verbose or ambiguous answers | One word, no punctuation |
| **Latency / tokens** | Longer completions | Shorter assistant output (1 token ideal) |

## Measurement protocol

1. Run the same 10 eval examples through baseline (`dataset/baseline.json`).
2. After fine-tune, run the same prompts with the fine-tuned model.
3. Compare:
   - exact match rate;
   - format violations (extra words, wrong category);
   - average completion length.

## Success definition

Fine-tuning is successful if **accuracy on eval ≥ 90%** and **format violations = 0** on the 10 baseline samples, without degrading on out-of-domain prompts (manual spot-check).
