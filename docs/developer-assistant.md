# Developer Assistant

## Purpose

The Developer Assistant is a CLI tool for answering questions about this repository.
It uses README.md and files from the docs directory as its RAG knowledge base.

## Commands

- `/help <question>` searches project documentation and asks the local Ollama model to answer.
- `/branch` returns the current git branch through MCP.
- `/status` returns concise git status through MCP.
- `/diff` returns staged and unstaged changes through MCP.
- `/files` returns the project file tree through MCP.

## Architecture

The CLI builds a temporary in-memory RAG index at startup. ProjectDocumentLoader loads README.md and docs files. MarkdownStructureChunker creates chunks. HashEmbeddingClient generates local embeddings. ImprovedRagRetriever selects relevant chunks. DeveloperAssistantPromptBuilder adds the retrieved context and current git branch to the prompt.

Git information is exposed by a local MCP server implemented by ProjectMcpServerFactory. Developer Assistant accesses it through RemoteMcpClient.
