# Playwright MCP smoke profile

Use the Playwright MCP server to execute every scenario from `testing/smoke/scenarios.md` against `SMOKE_BASE_URL`.

For every scenario you MUST:
1. Open a fresh browser context.
2. Execute steps exactly in order.
3. Take a screenshot after every step into `testing/smoke/screenshots`.
4. Record PASS or FAIL, actual result, failed selector/request, console errors and likely owning file.
5. Never claim PASS without checking the expected UI state.

Return one report with: scenario, steps, screenshots, result, failure analysis and likely source file.
