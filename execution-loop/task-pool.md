# Execution Loop Task Pool

Pool of 18 safe autonomous tasks for the Kotlin project.
Each task writes under `execution-loop/artifacts/`.

id: task-001
type: documentation
profile: documentation
description: Read build.gradle.kts and create execution-loop/artifacts/gradle-tasks.md with heading "# Gradle Tasks" and a bullet list of every tasks.register task name with its description.
output: execution-loop/artifacts/gradle-tasks.md
validation: file-contains
expected: # Gradle Tasks
commit: execution-loop task-001: document Gradle tasks

id: task-002
type: research
profile: research
description: Search for interface LlmClient and class OllamaClient, then create execution-loop/artifacts/llm-clients.md with heading "# LLM Clients" and sections for both clients including file paths.
output: execution-loop/artifacts/llm-clients.md
validation: file-contains
expected: # LLM Clients
commit: execution-loop task-002: document LLM clients

id: task-003
type: documentation
profile: documentation
description: Create execution-loop/artifacts/assistants.md listing all *Cli.kt entry points under src/main/kotlin with a one-line purpose. Use heading "# Assistant Entry Points".
output: execution-loop/artifacts/assistants.md
validation: file-contains
expected: # Assistant Entry Points
commit: execution-loop task-003: document assistant entry points

id: task-004
type: research
profile: research
description: Inspect TaskStateStorage and create execution-loop/artifacts/state-storage.md explaining how task-state.json is written. Use heading "# Task State Storage".
output: execution-loop/artifacts/state-storage.md
validation: file-contains
expected: # Task State Storage
commit: execution-loop task-004: document task state storage

id: task-005
type: documentation
profile: documentation
description: Create execution-loop/artifacts/project-packages.md listing top-level packages under ru.maleks.ai_advent_challenge_app. Use heading "# Project Packages".
output: execution-loop/artifacts/project-packages.md
validation: file-contains
expected: # Project Packages
commit: execution-loop task-005: document project packages

id: task-006
type: research
profile: research
description: Analyze GitProjectClient and GitDiffProvider, then create execution-loop/artifacts/git-tools.md comparing MCP-based and direct git access. Use heading "# Git Tools".
output: execution-loop/artifacts/git-tools.md
validation: file-contains
expected: # Git Tools
commit: execution-loop task-006: document git tools

id: task-007
type: documentation
profile: documentation
description: Create execution-loop/artifacts/mcp-servers.md documenting local MCP servers started from Main.kt. Use heading "# MCP Servers".
output: execution-loop/artifacts/mcp-servers.md
validation: file-contains
expected: # MCP Servers
commit: execution-loop task-007: document MCP servers

id: task-008
type: research
profile: research
description: Inspect LayeredMemoryAgent and create execution-loop/artifacts/memory-layers.md describing short-term, working and long-term memory. Use heading "# Memory Layers".
output: execution-loop/artifacts/memory-layers.md
validation: file-contains
expected: # Memory Layers
commit: execution-loop task-008: document memory layers

id: task-009
type: documentation
profile: documentation
description: Create execution-loop/artifacts/rag-cli.md documenting runRagIndex, runRagAsk and runRagChat Gradle tasks. Use heading "# RAG CLI".
output: execution-loop/artifacts/rag-cli.md
validation: file-contains
expected: # RAG CLI
commit: execution-loop task-009: document RAG CLI tasks

id: task-010
type: research
profile: research
description: Inspect ReleaseAssistant pipeline and create execution-loop/artifacts/release-pipeline.md with the five release steps. Use heading "# Release Pipeline".
output: execution-loop/artifacts/release-pipeline.md
validation: file-contains
expected: # Release Pipeline
commit: execution-loop task-010: document release pipeline

id: task-011
type: documentation
profile: documentation
description: Create execution-loop/artifacts/invariants.md summarizing project invariants from CLAUDE.md. Use heading "# Project Invariants".
output: execution-loop/artifacts/invariants.md
validation: file-contains
expected: # Project Invariants
commit: execution-loop task-011: document project invariants

id: task-012
type: research
profile: research
description: Search for AssistantProfile usages and create execution-loop/artifacts/profiles.md describing BugFix, Research and Architecture profiles. Use heading "# Assistant Profiles".
output: execution-loop/artifacts/profiles.md
validation: file-contains
expected: # Assistant Profiles
commit: execution-loop task-012: document assistant profiles

id: task-013
type: documentation
profile: documentation
description: Create execution-loop/artifacts/test-suite.md listing all *Test.kt files under src/test/kotlin. Use heading "# Test Suite".
output: execution-loop/artifacts/test-suite.md
validation: file-contains
expected: # Test Suite
commit: execution-loop task-013: document test suite

id: task-014
type: feature
profile: architecture
description: Create execution-loop/artifacts/execution-loop-architecture.md describing the executionloop package components and their responsibilities. Use heading "# Execution Loop Architecture".
output: execution-loop/artifacts/execution-loop-architecture.md
validation: file-contains
expected: # Execution Loop Architecture
commit: execution-loop task-014: document execution loop architecture

id: task-015
type: refactor
profile: architecture
description: Create execution-loop/artifacts/cli-map.md mapping Gradle tasks to main classes in a Markdown table. Use heading "# CLI Map".
output: execution-loop/artifacts/cli-map.md
validation: file-contains
expected: # CLI Map
commit: execution-loop task-015: document CLI map

id: task-016
type: test
profile: documentation
description: Create execution-loop/artifacts/validation-rules.md explaining execution loop validation kinds file-exists, file-contains, invariants and gradle-test. Use heading "# Validation Rules".
output: execution-loop/artifacts/validation-rules.md
validation: file-contains
expected: # Validation Rules
commit: execution-loop task-016: document validation rules

id: task-017
type: bug-fix
profile: bugfix
description: Create execution-loop/artifacts/troubleshooting.md with common execution loop failure categories and remediation steps. Use heading "# Troubleshooting".
output: execution-loop/artifacts/troubleshooting.md
validation: file-contains
expected: # Troubleshooting
commit: execution-loop task-017: add troubleshooting guide

id: task-018
type: documentation
profile: documentation
description: Create execution-loop/artifacts/run-summary-template.md with a template for recording run metrics including consecutive tasks, break reason and average duration. Use heading "# Run Summary Template".
output: execution-loop/artifacts/run-summary-template.md
validation: file-contains
expected: # Run Summary Template
commit: execution-loop task-018: add run summary template
