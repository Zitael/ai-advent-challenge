# Day 13 — Gateway Input Guard Test Results

Passed: **11/11**

| ID | Description | Expected | Actual | Findings | Pass |
|---|---|---|---|---|---|
| tc-001 | Clean prompt without secrets | ALLOW | ALLOW |  | PASS |
| tc-002 | OpenAI API key in prompt | BLOCK | BLOCK | API_KEY_OPENAI | PASS |
| tc-003 | GitHub PAT | BLOCK | BLOCK | API_KEY_GITHUB | PASS |
| tc-004 | AWS access key | BLOCK | BLOCK | API_KEY_AWS | PASS |
| tc-005 | Credit card number | BLOCK | BLOCK | CREDIT_CARD;PHONE | PASS |
| tc-006 | Email address | BLOCK | BLOCK | EMAIL | PASS |
| tc-007 | Phone number | BLOCK | BLOCK | PHONE | PASS |
| tc-008 | Base64-encoded OpenAI key | BLOCK | BLOCK | BASE64_SECRET | PASS |
| tc-009 | Split secret across parts | BLOCK | BLOCK | API_KEY_OPENAI | PASS |
| tc-010 | Mask mode instead of block | MASK | MASK | API_KEY_OPENAI | PASS |
| tc-011 | Multiple secrets in one prompt | BLOCK | BLOCK | API_KEY_GITHUB;API_KEY_OPENAI | PASS |
