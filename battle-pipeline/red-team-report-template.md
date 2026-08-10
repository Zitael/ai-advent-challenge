# Red Team Report Template

**Attacker:** _______________  
**Defender:** _______________  
**Target URL:** _______________  
**Date:** _______________

## Attacks attempted

| # | Vector | Payload (short) | Result | Layer that blocked | Evidence |
|---|---|---|---|---|---|
| 1 | Prompt injection | | blocked / passed | | screenshot/log |
| 2 | Indirect injection | | | | |
| 3 | Security bypass | | | | |
| 4 | Gateway secret leak | | | | |
| 5 | Custom | | | | |

## What worked (bypassed defenses)

_Describe successful attacks with exact payload and response._

## What failed (blocked)

_List blocked attempts and which guard caught them._

## Logs / screenshots

- Gateway audit: `llm-gateway/logs/audit.jsonl`
- Browser network tab
- Server response bodies

## Defender fix plan

| Finding | Fix | Retest result |
|---|---|---|
| | | |

## Final retest (after fixes)

Same attacks re-run: closed / still open
