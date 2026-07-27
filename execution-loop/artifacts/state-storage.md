# Task State Storage

Файл `task-state.json` используется для хранения состояния задач в процессе выполнения цикла. Он записывается через класс `TaskStateStorage`, который использует Jackson для сериализации и десериализации данных.

## Ключевые файлы
- `src/main/kotlin/ru/maleks/ai_advent_challenge_app/state/TaskStateStorage.kt` - реализация хранилища состояния задач.
- `src/test/kotlin/ru/maleks/ai_advent_challenge_app/executionloop/ExecutionLoopTest.kt` - тестирование работы с `TaskStateStorage`.
- `src/main/kotlin/ru/maleks/ai_advent_challenge_app/Main.kt` - инициализация `TaskStateStorage`.

## Как устроено
Класс `TaskStateStorage` содержит методы для чтения и записи состояния задач в файл. Он использует Jackson для сериализации объекта состояния в JSON-формат, который сохраняется в файл `task-state.json`. 

## Связи и поток данных
1. При запуске приложения создается экземпляр `TaskStateStorage`.
2. `TaskStateStorage` используется в `TaskStateMachine`, чтобы отслеживать состояние задач.
3. В тестах проверяется, что `TaskStateStorage` корректно записывает и считывает данные из файла.

## Выводы
Файл `task-state.json` является важной частью системы управления состоянием задач. Он позволяет сохранять прогресс выполнения задач между запусками приложения.

## Что осталось неизвестным
- Точная структура данных, хранящаяся в `task-state.json`.
- Какие именно задачи отслеживаются и как они обновляются.