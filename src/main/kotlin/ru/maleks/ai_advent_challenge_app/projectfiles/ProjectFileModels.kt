package ru.maleks.ai_advent_challenge_app.projectfiles

import java.nio.file.Path

data class FileMatch(
    val path: Path,
    val lineNumber: Int,
    val line: String
)

data class FileChange(
    val path: Path,
    val before: String?,
    val after: String
)

data class ProjectTaskResult(
    val summary: String,
    val inspectedFiles: List<Path>,
    val changes: List<FileChange> = emptyList()
)
