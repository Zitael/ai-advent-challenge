# Day 9 — Сценарий для видео (5–7 мин)

## 1. Проблема (45 сек)

Один промпт на triage с 4 полями ломается на:

- multi-domain тикетах (`complex-001`)
- noisy input (`noisy-001`)
- conditional policy (`policy-001` → auto_reply vs human_review)

Тезис: **decomposition = меньше cognitive load на каждый вызов**.

## 2. Два варианта (1 мин)

**A — Monolithic** (`MonolithicTriageService`):

```
ticket → [1 big LLM] → TOON result
```

**B — Multi-stage** (`MultiStageTriagePipeline`):

```
ticket → analyze (mini) → decide (4o) → format (mini) → TOON result
```

Показать strict TOON форматы в `DecompositionPrompts.kt`.

## 3. Live demo (2–3 мин)

```powershell
.\gradlew.bat runDecompositionEvaluation --console=plain
```

Комментировать:

```
Variant A — monolithic
  Valid format: X/8
  Matched fields: Y/24

Variant B — multi-stage
  Valid format: X/8
  Matched fields: Y/24
```

Открыть `decomposition/report.json`:

- `complex-001` — сравнить mono vs multi matched fields
- `rawStages` — показать 3 коротких ответа этапов

## 4. Trade-offs (1 мин)

| | Monolithic | Multi-stage |
|---|---|---|
| Calls | 1 | 3 |
| Latency | lower | higher |
| Format validity | часто хуже на noise | лучше (normalize first) |
| Model mix | one model | cheap + strong + cheap |
| Cost | lower per ticket | decide on strong, analyze/format cheap |

Формула: multi-stage дороже по latency, но **strong model только на decide**.

## 5. Связка с Day 7–8 (30 сек)

- Day 7: quality gates на одном ответе
- Day 8: routing cheap → strong
- Day 9: **pipeline** — разные модели на разных subtasks

## 6. Takeaway (30 сек)

- Decomposition работает когда subtasks имеют **разный формат и сложность**
- Strict TOON на каждом этапе → parseable, testable
- Stage 3 можно заменить кодом — но LLM полезен для summary

## Подготовка

1. Прогнать evaluation до записи
2. Открыть: `MultiStageTriagePipeline.kt`, `CompactToonParser.kt`, `report.json`
