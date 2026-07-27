# Git Tools

## Краткий ответ
The project uses two approaches for interacting with Git: **MCP-based** (via `GitProjectClient` and `GitDiffProvider`) and **direct git access** (via `GitDiffProvider`). The MCP approach abstracts git operations, while direct access provides lower-level control.

## Ключевые файлы
- `src/main/kotlin/ru/maleks/ai_advent_challenge_app/developer/GitProjectClient.kt` - Abstracts Git operations using MCP.
- `src/main/kotlin/ru/maleks/ai_advent_challenge_app/developer/GitDiffProvider.kt` - Provides direct git diff access.
- `src/main/kotlin/ru/maleks/ai_advent_challenge_app/developer/DeveloperAssistant.kt` - Uses both GitProjectClient and GitDiffProvider for git operations.

## Как устроено
### MCP-based Git Access
- **GitProjectClient**: Abstracts git operations like branch, status, diff, and files.
- **GitDiffProvider**: Provides direct access to git diffs and can be used to get pull request changes.
- These classes are injected into `DeveloperAssistant` for use in the CLI.

### Direct Git Access
- **GitDiffProvider** also provides direct access to git operations like getting local changes and pull request changes.
- This approach is more granular and allows for more control over git operations.

## Связи и поток данных
- The `DeveloperAssistant` class uses both `GitProjectClient` and `GitDiffProvider` to get git information.
- The `PullRequestReviewMain` class uses `GitDiffProvider` to get pull request changes.
- The `ReleaseAssistant` class uses `GitDiffProvider` for release-related git operations.

## Выводы
The project has two approaches for interacting with Git: **MCP-based** (via `GitProjectClient` and `GitDiffProvider`) and **direct git access** (via `GitDiffProvider`). The MCP approach abstracts git operations, while direct access provides lower-level control. Both approaches are used in different parts of the project.

## Что осталось неизвестным
- It is not clear how the MCP-based Git access is implemented.
- It is not clear what the exact differences are between the MCP-based and direct git access approaches.