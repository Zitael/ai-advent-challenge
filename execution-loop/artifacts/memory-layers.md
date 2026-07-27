# Memory Layers

The `LayeredMemoryAgent` is designed to manage multiple memory layers to support different aspects of conversation and task execution. Here's an overview of the short-term, working, and long-term memory layers based on available code and configuration:

## Key Files
- `src/main/kotlin/ru/maleks/ai_advent_challenge_app/agent/LayeredMemoryAgent.kt` - Core implementation of the layered memory agent.
- `.gitignore` - Indicates a file named `assistant-memory.json` which might be used for persistent memory storage.
- `docs/developer-assistant.md` - Describes how the CLI builds an in-memory RAG index and uses it for context retrieval.

## How It's Structured
- **Short-term Memory**: Likely used for immediate conversation context, such as recent messages or task-specific data. This is often cleared after each interaction.
- **Working Memory**: Used during active tasks to store intermediate results, current state of operations, or temporary data needed for processing.
- **Long-term Memory**: Persistent storage that retains information across sessions, potentially using files like `assistant-memory.json` for storing conversation history or learned patterns.

## Data Flow and Connections
- The `LayeredMemoryAgent` is initialized in the main application (`Main.kt`) and used to generate responses based on memory layers.
- The agent interacts with RAG (Retrieval-Augmented Generation) components, which use embeddings and document retrieval to augment context from long-term memory.

## Summary
The `LayeredMemoryAgent` manages short-term, working, and long-term memory layers to support dynamic conversation and task execution. Short-term memory handles immediate interaction data, working memory supports active tasks, and long-term memory retains persistent information across sessions. The exact implementation details for each layer are not fully visible in the provided code.

## What Remains Unknown
- The exact structure and implementation of each memory layer within `LayeredMemoryAgent`.
- How the agent interacts with external storage (e.g., `assistant-memory.json`) for long-term memory.
- Specific use cases or examples of how each memory layer is utilized in different scenarios.