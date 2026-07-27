# Release Pipeline

## 1. Prepare Release
- **Task**: `prepareRelease`
- **Description**: Run Day 35 AI release preparation pipeline
- **Main Class**: `ru.maleks.ai_advent_challenge_app.release.ReleaseAssistantCliKt`
- **Arguments**:
  - `--releaseVersion` (default: "day35-local")
  - `--skip-checks` (optional, if set to "true")

## 2. Validate Code Quality
- **Task**: `fullVerification`
- **Description**: Run code tests and UI smoke tests, then collect a unified report
- **Dependencies**:
  - `test`: Runs Kotlin unit tests
  - `runSmokeTests`: Runs Playwright smoke scenarios against Private AI UI

## 3. Build Artifacts
- **Task**: `build`
- **Description**: Gradle build task to compile and package the application
- **Includes**:
  - Compiles Kotlin source files
  - Packages into JAR or other artifact format

## 4. Run Release Checks
- **Task**: `prepareRelease` (continued)
- **Description**: Runs release-specific checks as part of the preparation pipeline
- **Includes**:
  - Code quality checks
  - Dependency checks
  - Configuration validation

## 5. Finalize and Deploy
- **Task**: Not explicitly defined, but implied by the flow
- **Description**: Final deployment steps (not detailed in current configuration)
- **May include**:
  - Artifact upload to repository
  - CI/CD pipeline trigger
  - Notification of release completion

## Summary:
The release pipeline consists of five key steps: preparing the release, validating code quality, building artifacts, running release checks, and finalizing/deploying. The `prepareRelease` task is central to this process, orchestrating much of the preparation work.