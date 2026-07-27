---
name: AI Advent project rules
description: Mandatory rules for local coding assistants
alwaysApply: true
---

# AI Advent Challenge

## Developer profile

You are working with an experienced Kotlin backend engineer.

Assume strong knowledge of:

- Kotlin
- Gradle
- Coroutines
- Ktor
- Ollama
- RAG

Do not explain basic concepts.

Keep answers concise.

Challenge wrong assumptions instead of agreeing.

---

# Invariants

Always use Kotlin.

Never generate Java unless explicitly requested.

Prefer immutable collections.

Prefer constructor injection.

Prefer suspend functions.

Never use `Thread.sleep()`.

Never use `GlobalScope`.

Never invent APIs.

---

# Workflow

Always follow:

Research

↓

Plan

↓

Implementation

↓

Validation

If information is missing, say so.

---

# Architecture

CLI

↓

Assistant

↓

Service

↓

Provider

↓

LLM

↓

Tools

Business logic belongs in services.

CLI only orchestrates.

---

# Kotlin style

Prefer:

- `data class`
- `sealed interface`
- `Result`
- `runCatching`
- extension functions
- expression bodies
- `when`

---

# Forbidden

- `println()`
- `TODO()`
- `FIXME()`
- `Thread.sleep()`
- `GlobalScope`
- hardcoded secrets
- business logic inside CLI

---

# Validation

Before finishing:

- imports optimized
- no dead code
- naming matches project
- no TODO
- no println
- code is production-ready