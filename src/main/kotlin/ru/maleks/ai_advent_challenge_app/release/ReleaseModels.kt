package ru.maleks.ai_advent_challenge_app.release

import java.nio.file.Path

data class ReleaseConfig(
    val version: String,
    val runChecks: Boolean,
    val outputDirectory: Path
)

data class CommandResult(
    val command: String,
    val exitCode: Int?,
    val output: String,
    val timedOut: Boolean
) {
    val successful: Boolean
        get() = !timedOut && exitCode == 0
}

data class ReleaseArtifacts(
    val outputDirectory: Path,
    val releaseNotes: Path,
    val changelog: Path,
    val review: Path,
    val invariants: Path,
    val report: Path
)

data class ReleasePipelineResult(
    val version: String,
    val changedFiles: List<String>,
    val blockers: List<String>,
    val warnings: List<String>,
    val artifacts: ReleaseArtifacts,
    val checks: List<CommandResult>
) {
    val ready: Boolean
        get() = blockers.isEmpty()
}
