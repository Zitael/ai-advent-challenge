# Validation Rules

## 1. File-Exists Validation

Ensures that required files exist in the project repository.

### Examples:
- `execution-loop/README.md` must exist
- `execution-loop/artifacts/invariants.md` must exist
- `execution-loop/artifacts/release-pipeline.md` must exist
- `execution-loop/artifacts/validation-rules.md` must exist

## 2. File-Contains Validation

Verifies that files contain specific content or patterns.

### Examples:
- `build.gradle.kts` must contain the correct Kotlin version
- `README.md` must contain project description and usage instructions
- `LICENSE` file must contain appropriate licensing information
- All source files must have proper copyright headers

## 3. Invariants Validation

Ensures that core constraints and rules are upheld throughout development.

### Examples:
- No hardcoded secrets in codebase
- All business logic resides in services
- CLI only orchestrates execution flow
- Prefer immutable collections
- Prefer constructor injection
- Prefer suspend functions
- Never use `Thread.sleep()` or `GlobalScope`
- Never invent APIs

## 4. Gradle-Test Validation

Validates that all test tasks are properly configured and executed.

### Examples:
- All unit tests must be in `src/test/kotlin/`
- Integration tests should be in `src/integrationTest/kotlin/`
- Test tasks must use JUnit 5 platform
- All test classes must have proper package structure
- Test coverage must meet minimum thresholds

## Summary:

Validation rules ensure project integrity, correctness and alignment with best practices. These rules cover file existence, content validation, invariant checks and Gradle test configurations to maintain a robust and maintainable codebase.