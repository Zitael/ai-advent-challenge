# Day 4 — Local Boost

## Goal

Connect a local LLM to the IDE as:

- autocomplete model;
- chat assistant;
- code editing assistant;
- local agent.

The local assistant uses:

- Ollama;
- Continue;
- Qwen2.5-Coder;
- optional DeepSeek-Coder-V2 Lite;
- project rules from `CLAUDE.md`.

## Files

```text
.continue/
├── config.yaml
└── rules/
    └── project-rules.md

local-llm/
├── Modelfile.qwen2.5-coder-7b
├── Modelfile.qwen2.5-coder-14b
├── Modelfile.deepseek-coder-v2
├── setup-models.ps1
└── benchmark/
    ├── tasks/
    │   ├── 01-feature.md
    │   ├── 02-agent.md
    │   └── 03-context.md
    ├── run-benchmark.ps1
    └── COMPARISON_TEMPLATE.md