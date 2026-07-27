# MCP Servers

## Overview

The project includes several local MCP (Model Context Protocol) servers that can be started from `Main.kt`. These servers are used to run various tasks related to RAG, Ollama models, and developer support. Below is a list of the MCP servers along with their descriptions and associated files.

## List of MCP Servers

1. **RagIndexCliKt**
   - Description: Builds local RAG document indexes.
   - File: `ru.maleks.ai_advent_challenge_app.rag.RagIndexCliKt`
   - Task: `runRagIndex`

2. **RagAskCliKt**
   - Description: Asks a question with and without RAG.
   - File: `ru.maleks.ai_advent_challenge_app.rag.RagAskCliKt`
   - Task: `runRagAsk`

3. **RagChatCliKt**
   - Description: Runs an interactive RAG chat with a local Ollama model.
   - File: `ru.maleks.ai_advent_challenge_app.rag.chat.RagChatCliKt`
   - Task: `runRagChat`

4. **OllamaDemoCliKt**
   - Description: Runs three requests against a local Ollama model.
   - File: `ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaDemoCliKt`
   - Task: `runOllamaDemo`

5. **OllamaOptimizationCliKt**
   - Description: Compares baseline and optimized Ollama profiles.
   - File: `ru.maleks.ai_advent_challenge_app.llm.ollama.OllamaOptimizationCliKt`
   - Task: `runOllamaOptimization`

6. **PrivateAiServerKt**
   - Description: Runs a private HTTP API backed by a local Ollama model.
   - File: `ru.maleks.ai_advent_challenge_app.privateai.PrivateAiServerKt`
   - Task: `runPrivateAiServer`

7. **DeveloperAssistantCliKt**
   - Description: Runs a Day 31 developer assistant with project RAG and Git MCP.
   - File: `ru.maleks.ai_advent_challenge_app.developer.DeveloperAssistantCliKt`
   - Task: `runDeveloperAssistant`

8. **PullRequestReviewMainKt**
   - Description: Reviews pull request diffs with RAG and Ollama.
   - File: `ru.maleks.ai_advent_challenge_app.developer.PullRequestReviewMainKt`
   - Task: `reviewPullRequest`

9. **SupportAssistantCliKt**
   - Description: Runs a Day 33 support assistant with RAG and CRM MCP.
   - File: `ru.maleks.ai_advent_challenge_app.support.SupportAssistantCliKt`
   - Task: `runSupportAssistant`

10. **ProjectFileAssistantCliKt**
    - Description: Runs a Day 34 assistant that searches, analyzes, and modifies project files.
    - File: `ru.maleks.ai_advent_challenge_app.projectfiles.ProjectFileAssistantCliKt`
    - Task: `runProjectFileAssistant`

11. **ReleaseAssistantCliKt**
    - Description: Runs a Day 35 AI release preparation pipeline.
    - File: `ru.maleks.ai_advent_challenge_app.release.ReleaseAssistantCliKt`
    - Task: `prepareRelease`

12. **ExecutionLoopCliKt**
    - Description: Runs a Day 5 autonomous execution loop over `task-pool.md`.
    - File: `ru.maleks.ai_advent_challenge_app.executionloop.ExecutionLoopCliKt`
    - Task: `runExecutionLoop`

## Summary

The MCP servers listed above are essential for running various tasks related to RAG, Ollama models, and developer support. Each server is associated with a specific file and task in the project. These servers can be started from `Main.kt` using the corresponding Gradle tasks.