# Task 1 — Feature generation

Add a new Ollama generation profile named `fastCode`.

Requirements:

- inspect the existing Ollama generation profiles;
- add the profile next to the existing profiles;
- profile name: `fast-code`;
- temperature: `0.1`;
- top_p: `0.8`;
- context size: `16384`;
- generated token limit: `2200`;
- repeat penalty: `1.1`;
- thinking must be disabled;
- do not modify the existing baseline and RAG profiles;
- add or update unit tests if profile tests exist;
- run relevant verification.

Before editing, inspect:

- project rules;
- `OllamaOptimizationProfiles`;
- `OllamaGenerationConfig`;
- `OllamaOptions`;
- related tests;
- Gradle test configuration.

Return:

1. inspected files;
2. changed files;
3. verification command;
4. actual command result;
5. remaining risks.

Do not invent files, tests or command results.