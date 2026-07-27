# Project Packages

The following is a list of top-level packages under `ru.maleks.ai_advent_challenge_app` based on the project structure and source files:

- `rag`: Contains RAG (Retrieval-Augmented Generation) related CLI tools for indexing documents, asking questions, and running interactive chats.
- `llm.ollama`: Implements interactions with local Ollama models, including demo requests, optimization comparisons, and model profiling.
- `privateai`: Provides a private HTTP API backed by a local Ollama model for secure AI interactions.
- `developer`: Includes tools for developer assistance, such as pull request reviews, project file analysis, and release preparation pipelines.
- `support`: Contains support assistant tools leveraging RAG and CRM MCP (Model Context Protocol) for customer interaction.
- `projectfiles`: Offers an assistant that searches, analyzes, and modifies project files using AI-driven insights.
- `executionloop`: Manages the autonomous execution loop over task-pool.md to process tasks in a queue or run specific tasks by name.

These packages are organized according to the architecture guidelines, with business logic contained within services and CLI tools orchestrating execution flows.

Summary: The project is structured into top-level packages that align with its architecture and functionality, ensuring separation of concerns and modularity.