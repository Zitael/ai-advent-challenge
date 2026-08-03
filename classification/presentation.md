# Day 7 — Сценарий для видео (5–7 мин)

## 1. Контекст (30 сек)

- Задача: **маршрутизация support-тикетов** — ошибка = не тот отдел, SLA, churn.
- Fine-tuning не используем (это Day 6). Фокус: **контроль качества на inference**.
- Тезис: *«Лучше не ответить автоматически, чем ответить неправильно»*.

## 2. Архитектура (1 мин)

Показать схему из `classification/README.md`:

```
Запрос → Scoring → Constraint → Redundancy (×3) → Self-check → Decision
                                              ↓
                                    OK / UNSURE / FAIL / REJECTED
```

Четыре подхода из задания — не «или», а **слои защиты**:

1. **Scoring** — модель сама оценивает уверенность (`OK/UNSURE/FAIL`).
2. **Constraint** — дешёвая проверка без LLM: один токен, допустимая категория.
3. **Redundancy** — 3 прогона, majority vote 2/3.
4. **Self-check** — модель проверяет свой ответ (`YES` или исправление).

## 3. Live demo (2–3 мин)

```powershell
cd D:\src\ai_advent_app
.\gradlew.bat runConfidenceInference --console=plain
```

На экране комментировать:

| Что показать | Зачем |
|---|---|
| `Cases: 10` | 4 correct + 3 edge + 3 noisy |
| `Accepted (OK)` | Сколько прошло все gate |
| `Rejected / UNSURE` | Система **отказалась** маршрутизировать |
| `Retried` | Повторный inference при первом non-OK |
| Latency / token multiplier | Цена качества (~5–10× к baseline) |

Открыть `classification/report.json` (актуальный прогон):

- **`correct-*`** — все 4 gate passed → `OK`.
- **`noisy-002`** — scoring `UNSURE`, self-check не подтвердил → **отклонён**, `UNSURE` (human queue).
- **`noisy-001`** — все слои согласны на `technical`, но эталон `account` → показать **лимит guardrails**: unanimous wrong answer не спасает constraint/redundancy.
- **`edge-*`** — модель уверенно выбирает одну категорию; обсудить, что для ambiguous кейсов нужен порог scoring или эскалация по умолчанию.

Фактические метрики последнего прогона:

- Accepted: **9/10**, Rejected: **1/10**, UNSURE: **1/10**, Retried: **1/10**
- Latency **~4.8×**, tokens **~6.6×** vs baseline
- Correct accepted: **4/5** (на кейсах с эталоном)

## 4. Сравнение с baseline (1 мин)

Baseline = 1 вызов LLM без контроля (тот же gateway, что в Day 6).

Аргумент для видео:

- Baseline быстрее и дешевле, но **всегда возвращает категорию** — даже когда не уверен.
- Confidence pipeline **снижает false positive** за счёт latency/cost.
- Метрика успеха Day 7 — не accuracy%, а **доля безопасных auto-routes** + явный статус для остальных.

## 5. Критерии «стало лучше» (30 сек)

Из отчёта:

- ↓ accepted wrong routes на edge/noisy (смотрим `correctAcceptedCount` vs blind baseline).
- ↑ доля `UNSURE` вместо silent wrong answer.
- Explicit `REJECTED` при broken format.

## 6. Production takeaway (30 сек)

- Constraint — всегда первым (бесплатно).
- Redundancy — когда cost LLM << cost ошибки.
- Self-check / scoring — когда нужен explainable status для UI («отправить оператору»).
- Retry — один раз, не бесконечный loop.

## 7. Связка с Day 6 (15 сек)

Day 6: data + fine-tune prep. Day 7: **guardrails без retrain**. В проде обычно оба слоя.

## Что подготовить до записи

1. `OPENROUTER_API_KEY` в `.env`.
2. Прогнать команду один раз — report.json уже будет (нет cold start на видео).
3. Держать открытыми: `ConfidenceOrchestrator.kt`, `test-cases.json`, `report.json`.

## Возможные вопросы

**Почему 4 подхода, а не 2?**  
Задание требует минимум 2; 4 показывает trade-off: cheap rules + expensive redundancy + semantic self-check.

**Почему retry?**  
Stochastic sampling иногда стабилизируется на втором проходе; важно считать cost.

**Fine-tuning не нужен?**  
Для Day 7 — да. Fine-tune улучшает accuracy, confidence layer снижает risk оставшихся ошибок.
