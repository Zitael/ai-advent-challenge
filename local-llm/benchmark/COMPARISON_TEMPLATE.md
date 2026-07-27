# Day 4 — Local Boost comparison

## Hardware

| Field | Value |
|---|---|
| CPU | |
| RAM | |
| GPU | |
| VRAM | |
| Operating system | |
| Ollama version | |
| Continue version | |
| IDE | |

## Model configuration

| Model | Purpose | Context | Temperature | top_p |
|---|---|---:|---:|---:|
| Qwen2.5-Coder 1.5B Base | Autocomplete | 4096 | 0.05 | 0.8 |
| Qwen2.5-Coder 7B | Chat and edit | 16384 | 0.10 | 0.8 |
| Qwen2.5-Coder 14B | Chat and edit | 16384 | 0.10 | 0.8 |
| DeepSeek-Coder-V2 Lite | Chat and edit | 16384 | 0.10 | 0.8 |
| Cloud assistant | Chat and agent | Provider default | Provider default | Provider default |

## Scoring

- `0` — failed, produced unusable code or invented project facts;
- `1` — partially useful, but substantial manual correction was needed;
- `2` — mostly correct, one or two small corrections were needed;
- `3` — correct on the first attempt and confirmed by verification.

## Results

| Assistant | Feature /3 | Agent /3 | Context /3 | Average time | Offline | Manual corrections |
|---|---:|---:|---:|---:|---|---|
| Qwen2.5-Coder 7B | | | | | Yes | |
| Qwen2.5-Coder 14B | | | | | Yes | |
| DeepSeek-Coder-V2 Lite | | | | | Yes | |
| Cloud assistant | | | | | No | |

## Detailed results

### Qwen2.5-Coder 7B

#### Feature task

- Completed on first attempt:
- Correct files found:
- Code compiled:
- Tests passed:
- Manual corrections:
- Time:
- Notes:

#### Agent task

- Root cause found:
- Correct files found:
- Code compiled:
- Tests passed:
- Manual corrections:
- Time:
- Notes:

#### Context task

- Correct flow described:
- Invented components:
- Missing components:
- Time:
- Notes:

---

### Qwen2.5-Coder 14B

#### Feature task

- Completed on first attempt:
- Correct files found:
- Code compiled:
- Tests passed:
- Manual corrections:
- Time:
- Notes:

#### Agent task

- Root cause found:
- Correct files found:
- Code compiled:
- Tests passed:
- Manual corrections:
- Time:
- Notes:

#### Context task

- Correct flow described:
- Invented components:
- Missing components:
- Time:
- Notes:

---

### DeepSeek-Coder-V2 Lite

#### Feature task

- Completed on first attempt:
- Correct files found:
- Code compiled:
- Tests passed:
- Manual corrections:
- Time:
- Notes:

#### Agent task

- Root cause found:
- Correct files found:
- Code compiled:
- Tests passed:
- Manual corrections:
- Time:
- Notes:

#### Context task

- Correct flow described:
- Invented components:
- Missing components:
- Time:
- Notes:

---

### Cloud assistant

#### Feature task

- Completed on first attempt:
- Correct files found:
- Code compiled:
- Tests passed:
- Manual corrections:
- Time:
- Notes:

#### Agent task

- Root cause found:
- Correct files found:
- Code compiled:
- Tests passed:
- Manual corrections:
- Time:
- Notes:

#### Context task

- Correct flow described:
- Invented components:
- Missing components:
- Time:
- Notes:

## Final recommendation

### Local model is sufficient for

- autocomplete;
- small isolated Kotlin methods;
- DTO and test generation;
- boilerplate;
- refactoring one or two known classes;
- explanation of explicitly selected files;
- private offline development.

### Cloud assistant is preferable for

- repository-wide agent work;
- long tool-driven flows;
- ambiguous architecture changes;
- debugging across many files;
- tasks requiring several verification iterations;
- cases where first-attempt correctness matters more than offline operation.

## Best local setup on this machine

Model:

Autocomplete model:

Context size:

Temperature:

top_p:

Average generation speed:

Reason for choosing this configuration: