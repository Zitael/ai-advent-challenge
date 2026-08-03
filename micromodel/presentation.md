# Day 10 — Сценарий для видео (4–6 мин)

## 1. Проблема (30 сек)

LLM на каждый тикет = latency + cost, хотя 60–70% кейсов очевидны по ключевым словам.

Тезис: **micro-model first, LLM only when needed**.

## 2. Архитектура (1 мин)

```
Ticket → Keyword Micro-Classifier (local, ~0ms)
              ↓ OK + confidence ≥ 0.65
           accept label
              ↓ UNSURE / low confidence / bad format
           gpt-4o fallback
```

Показать `TicketKeywordMicroClassifier.kt` — это простой ML-классификатор на rules + scoring.

## 3. Live demo (2 мин)

```powershell
.\gradlew.bat runMicroModelEvaluation --console=plain
```

Комментировать метрики (актуальный прогон, 30 кейсов):

```
Micro-model handled: 17/30
Fallback used: 13/30
LLM calls: 13
Avg latency: ~388 ms
Latency saved: ~15s vs always-LLM

By kind:
- SIMPLE: 15/15 micro
- EDGE: 1/8 micro, 7 fallback
- COMPLEX: 1/7 micro, 6 fallback
```

Разбивка by kind: SIMPLE → micro, COMPLEX → fallback.

## 4. Примеры из report.json (1 мин)

| Case | Micro | Result |
|---|---|---|
| `simple-001` | OK, billing, 0.85 | no LLM |
| `edge-001` | UNSURE (mixed domains) | fallback |
| `complex-002` | UNSURE (no signals) | fallback |

## 5. Trade-offs (30 сек)

| | Always LLM | Micro-first |
|---|---|---|
| LLM calls | 28 | ~10 |
| Latency | high | low on simple |
| Edge accuracy | higher | depends on fallback |
| Cost | max | ~60% savings |

Micro-model не заменяет LLM — она **фильтрует поток**.

## 6. Связка с Day 7–9 (30 сек)

- Day 7: confidence gates
- Day 8: cheap → strong routing
- Day 9: multi-stage decomposition
- Day 10: **zero-cost micro layer** before any LLM

## Подготовка

1. Прогнать evaluation до записи
2. Открыть: `MicroFirstPipeline.kt`, `test-cases.json`, `report.json`
