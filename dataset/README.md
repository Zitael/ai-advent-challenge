# Day 6 — Fine-Tuning Dataset

## Task type

**Classification** — support ticket routing into CRM categories.

## Files

| File | Purpose |
|---|---|
| `raw/real-tickets.json` | Real labeled examples (CRM + manual) |
| `train.jsonl` | 80% training split |
| `eval.jsonl` | 20% evaluation split |
| `baseline.json` | 10 baseline answers from gpt-4o-mini without fine-tune |
| `evaluation-criteria.md` | How to measure improvement |

## Build

```powershell
.\gradlew.bat buildDataset --console=plain
.\gradlew.bat validateDataset --console=plain
.\gradlew.bat runDatasetBaseline --console=plain
```

## Fine-tuning client (prepare only)

```powershell
.\gradlew.bat prepareFineTuning --console=plain
```

Requires `OPENAI_API_KEY`. Does **not** start a job unless `--execute` is passed.

## JSONL format

Each line:

```json
{
  "messages": [
    {"role": "system", "content": "..."},
    {"role": "user", "content": "Ticket: ..."},
    {"role": "assistant", "content": "technical"}
  ]
}
```
