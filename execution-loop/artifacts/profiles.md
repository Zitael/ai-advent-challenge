# Assistant Profiles

## Краткий ответ
The project defines three assistant profiles: BugFix, Research, and Architecture. These are used to categorize tasks in the execution loop and determine which assistant (CLI) should handle them.

## Ключевые файлы
- `src/main/kotlin/ru/maleks/ai_advent_challenge_app/developer/profile/AssistantProfile.kt` - defines the sealed class `AssistantProfile` with three data objects: `BugFix`, `Research`, and `Architecture`.
- `src/main/kotlin/ru/maleks/ai_advent_challenge_app/developer/DeveloperCommand.kt` - contains the `BugFix` data class for command parsing.
- `execution-loop/task-pool.md` - references these profiles in task definitions.

## Как устроено
The `AssistantProfile` is a sealed class with three distinct data objects representing different types of tasks:
1. **BugFix** - For finding and fixing bugs, often involving test execution.
2. **Research** - For exploring the codebase or understanding complex issues.
3. **Architecture** - For architectural decisions or improvements.

These profiles are used in the CLI to route commands to the appropriate assistant logic.

## Связи и поток данных
- The `DeveloperAssistantCli.kt` file uses these profiles to execute different types of tasks based on user input.
- Tasks defined in `task-pool.md` reference these profiles to determine which CLI should handle them.
- The `profileAssistant.execute()` method is called with the appropriate profile and task/command.

## Выводы
The project has a well-defined system for categorizing tasks using assistant profiles. This allows for modular handling of different types of requests in the execution loop.

## Что осталось неизвестным
- The exact implementation details of how each profile (BugFix, Research, Architecture) is handled in the CLI.
- Whether these profiles are used in other parts of the codebase beyond the developer assistant.