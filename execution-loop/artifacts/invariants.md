# Project Invariants

## Overview

The project invariants are derived from the `CLAUDE.md` document and reflect the core constraints and rules that must be upheld throughout the development lifecycle. These invariants ensure consistency, maintainability, and alignment with best practices for Kotlin and Ktor-based applications.

## Invariant Details

### 1. Language and Framework Constraints
- **Kotlin Only**: All code must be written in Kotlin.
- **No Java**: Avoid using Java unless explicitly requested.
- **Ktor**: The project uses Ktor as the primary web framework.
- **Coroutines**: Prefer suspend functions and coroutines for asynchronous operations.

### 2. Architecture and Structure
- **CLI Orchestrates**: CLI is responsible for orchestrating tasks, while business logic resides in services.
- **Service Layer**: Business logic must be encapsulated within service layers.
- **Provider Layer**: Providers handle data access or external interactions.
- **LLM Integration**: LLMs are integrated via tools and should not contain business logic.

### 3. Code Quality and Style
- **Immutable Collections**: Prefer immutable collections where possible.
- **Constructor Injection**: Favor constructor injection for dependency management.
- **Result Type**: Use `Result` type for error handling.
- **runCatching**: Use `runCatching` for safe execution of potentially failing operations.
- **Extension Functions**: Prefer extension functions for utility and readability.
- **Expression Body**: Use expression bodies where applicable.
- **Sealed Interfaces**: Use sealed interfaces for closed sets of types.
- **Data Classes**: Favor data classes for holding data.

### 4. Development Practices
- **No Hardcoded Secrets**: Avoid hardcoded secrets; use environment variables or secure storage.
- **No TODO/FIXME**: Eliminate `TODO()` and `FIXME()` in production code.
- **No println()**: Avoid using `println()` for output; use proper logging mechanisms.
- **No Thread.sleep()**: Avoid blocking operations like `Thread.sleep()`.
- **No GlobalScope**: Avoid using `GlobalScope` for coroutine launches.

### 5. Testing and Validation
- **Gradle Test Task**: Ensure all unit tests are covered by the Gradle test task.
- **Validation Rules**: Implement validation rules such as file-exists, file-contains, invariants, and gradle-test to ensure correctness.
- **No Dead Code**: Eliminate dead code to maintain a clean and efficient codebase.

### 6. File and Naming Conventions
- **Naming Matches Project**: Ensure all files and classes follow the project's naming conventions.
- **File Paths**: Use consistent and meaningful file paths that reflect the project structure.

## Summary

The invariants ensure that the project remains consistent, maintainable, and aligned with best practices for Kotlin and Ktor development. They enforce language constraints, architectural patterns, code quality standards, and development practices to support a robust and scalable application.