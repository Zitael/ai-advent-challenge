# LLM Clients

## Interface LlmClient
- **File path**: `src/main/kotlin/ru/maleks/ai_advent_challenge_app/llm/LlmClient.kt`
- **Definition**:
  ```kotlin
  interface LlmClient {
      suspend fun complete(request: LlmRequest): LlmResponse
  }
  ```
- **Usage**:
  - Used by `LayeredMemoryAgent` for generating responses.
  - Implemented by `OllamaClient`.

## Class OllamaClient
- **File path**: `src/main/kotlin/ru/maleks/ai_advent_challenge_app/llm/ollama/OllamaClient.kt`
- **Definition**:
  ```kotlin
  class OllamaClient(
      private val client: HttpClient,
      private val model: String
  ) : LlmClient {
      override suspend fun complete(request: LlmRequest): LlmResponse {
          // Implementation for calling Ollama API
      }
  }
  ```
- **Usage**:
  - Used by `CodeReviewService` and `DeveloperAssistant` for interacting with the local Ollama model.
  - Configured via `OllamaDemoCliKt`, `OllamaOptimizationCliKt`, and `PrivateAiServerKt`.

## Related Tasks
- **Tasks using LlmClient**:
  - `runOllamaDemo`
  - `runOllamaOptimization`
  - `runPrivateAiServer`
  - `runDeveloperAssistant`
  - `reviewPullRequest`

Summary: The project defines an interface `LlmClient` and a class `OllamaClient` that implements it. These are used to interact with LLMs, specifically Ollama models, across various services and CLI tasks.