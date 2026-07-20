# Day 35 — Release Assistant

## Real task

The assistant automates release preparation for the current project. It collects local Git changes, scans project files for invariant violations, optionally runs Gradle checks, asks a local Ollama model to produce release notes and a final release review, and saves reproducible artifacts.

## AI role

AI receives the changed-file list, bounded Git diff, inspected source context, invariant findings, and check status. It produces:

- `RELEASE_NOTES.md` for users;
- `AI_REVIEW.md` with risks and a release recommendation.

Deterministic tooling remains responsible for Git, filesystem access, invariant checks, Gradle execution, and file persistence.

## Run

Windows:

```bat
gradlew.bat prepareRelease -PreleaseVersion=1.0.0 --console=plain
```

Faster demo without test/build:

```bat
gradlew.bat prepareRelease -PreleaseVersion=demo -PskipChecks=true --console=plain
```

Artifacts are written to:

```text
build/release/<version>/
```

Generated files:

- `RELEASE_NOTES.md`
- `CHANGELOG.md`
- `AI_REVIEW.md`
- `INVARIANTS.md`
- `RELEASE_REPORT.md`

## Presentation

1. Make or keep several local code changes.
2. Run `prepareRelease`.
3. Show that the assistant itself collects Git changes, checks multiple files, runs real project commands, calls Ollama, and writes five release artifacts.
4. Open `RELEASE_REPORT.md` and `RELEASE_NOTES.md`.

This is a real release-preparation pipeline rather than a text-only chatbot: AI makes contextual decisions and writes release content, while tooling performs verifiable actions in the project environment.
