# Day 8 — Сценарий для видео (4–6 мин)

## 1. Проблема (30 сек)

- Сильная модель точнее, но дороже и медленнее.
- Слабая модель покрывает ~70–80% простых кейсов.
- **Routing** = платим за strong только когда cheap не уверен.

## 2. Стратегия (1 мин)

```
Ticket → Cheap (scoring) → Heuristics OK? → accept
                              ↓ fail
                         Strong (classify) → accept
```

Три эвристики (показать `RoutingHeuristics.kt`):

1. **confidence < 0.8**
2. **answer length / format** — один токен, допустимая длина
3. **unsure rule** — status != OK → эскалация

## 3. Live demo (2 мин)

```powershell
.\gradlew.bat runModelRouting --console=plain
```

Комментировать вывод (актуальный прогон):

```
Cheap model only: 4/10
Escalated to strong: 6/10
Per-case routing:
- simple-*: CHEAP (4 кейса)
- edge-* / noisy-*: STRONG (6 кейсов)
Correct: 4/5
Total cost: ~$0.0014
```

Открыть `routing/report.json` — поля `escalated`, `escalationReasons`, `cheapConfidence`.

## 4. Cost / latency trade-off (1 мин)

Из отчёта:

- SIMPLE кейсы — 1 cheap call (~400 tokens)
- Escalated — cheap + strong (~400 + 200 tokens)
- **Total cost** vs гипотетический «всё на strong» (10 × strong)

Формула для видео:  
`экономия ≈ (cheapOnlyCount × strongPrice) - cheapOnlyCost`

## 5. Связка с Day 7 (30 сек)

| Day 7 | Day 8 |
|---|---|
| Много вызовов одной модели | Разные модели по уверенности |
| Gate / reject | Escalate / fallback |
| Quality control | Cost optimization |

Можно комбинировать: routing → confidence gates на strong path.

## 6. Takeaway (30 сек)

- Routing — это **policy layer**, не magic.
- Эвристики должны быть observable (`escalationReasons` в report).
- Тюнинг порога confidence — главный lever.

## Подготовка

1. Прогнать `runModelRouting` до записи.
2. Держать открытыми: `ModelRouter.kt`, `RoutingHeuristics.kt`, `report.json`.
