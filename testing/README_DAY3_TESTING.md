# Day 3 — Testing

## Level 1: code tests
Three previously uncovered modules now have tests:
- `ApiKeyValidatorTest`
- `InMemoryRateLimiterTest`
- `TaskStateMachineTest`

Run: `./gradlew test`

Agent prompt:
"Find uncovered business modules, select at least three deterministic modules, write tests without changing production behaviour, run the full test suite, and report files, cases and command output. Do not claim success unless Gradle exits with code 0."

## Level 2: UI smoke
Start app: `./gradlew runPrivateAiServer`
Run smoke: `./gradlew runSmokeTests`
Playwright MCP profile: `testing/smoke/playwright-mcp-profile.md`
Scenarios: `testing/smoke/scenarios.md`
Screenshots: `testing/smoke/screenshots/`

## Unified flow after PR
Run: `./gradlew fullVerification`

For a deployed feature:
"Inspect the changed UI/API files, update `testing/smoke/scenarios.md` and `private-ai.smoke.spec.js`, then run `fullVerification`. Produce one report containing code tests, smoke scenarios, screenshots and likely fault location for every failure."
