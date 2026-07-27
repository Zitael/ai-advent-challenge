# Unified verification report

## Code tests
- Command: `./gradlew test`
- Result: PASS / FAIL
- Tested modules:
  - TaskStateMachine
  - ApiKeyValidator
  - InMemoryRateLimiter
- Failed tests and likely source files: none / details

## UI smoke
- Base URL: `SMOKE_BASE_URL`
- Command: `./gradlew runSmokeTests`
- Result: PASS / FAIL

| Scenario | Result | Screenshots | Likely fault location |
|---|---|---|---|
| SMOKE-01 health | | | `PrivateAiServer.kt`, `index.html` |
| SMOKE-02 settings | | | `index.html` |
| SMOKE-03 required key | | | `index.html` |
| SMOKE-04 unauthorized | | | `ApiKeyValidator.kt`, `PrivateAiServer.kt`, `index.html` |
| SMOKE-05 clear validation | | | `index.html` |

## Final conclusion
- PR can be merged: YES / NO
- Blocking failures:
- Recommended fix location:
