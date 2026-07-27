package ru.maleks.ai_advent_challenge_app.executionloop

import java.nio.file.Path
import java.time.Duration
import java.time.Instant

data class ExecutionTask(
    val id: String,
    val description: String,
    val outputPath: Path?,
    val validation: ExecutionValidationKind,
    val expectedContent: String? = null
)

enum class ExecutionValidationKind {
    FILE_EXISTS,
    FILE_CONTAINS,
    INVARIANTS
}

enum class ExecutionTaskStatus {
    SUCCESS,
    FAILED,
    SKIPPED
}

data class ExecutionAttemptRecord(
    val taskId: String,
    val taskDescription: String,
    val startedAt: Instant,
    val attemptNumber: Int,
    val agentResult: String,
    val validationResult: String,
    val status: ExecutionTaskStatus,
    val duration: Duration
)

data class ExecutionRunSummary(
    val runId: String,
    val startedAt: Instant,
    val finishedAt: Instant,
    val totalTasks: Int,
    val completedTasks: Int,
    val failedTasks: Int,
    val stoppedEarly: Boolean,
    val stopReason: String?,
    val attempts: List<ExecutionAttemptRecord>
)
