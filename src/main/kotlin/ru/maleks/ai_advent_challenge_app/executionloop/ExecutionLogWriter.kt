package ru.maleks.ai_advent_challenge_app.executionloop

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ExecutionLogWriter(
    private val logDirectory: Path,
    private val runId: String
) {
    private val logFile: Path = logDirectory.resolve("$runId.md")
    private val attempts = mutableListOf<ExecutionAttemptRecord>()

    fun recordAttempt(record: ExecutionAttemptRecord) {
        attempts += record
        Files.createDirectories(logDirectory)
        Files.writeString(
            logFile,
            renderAttempts(),
            StandardCharsets.UTF_8
        )
    }

    fun writeSummary(summary: ExecutionRunSummary) {
        Files.createDirectories(logDirectory)
        Files.writeString(
            logFile,
            renderFullLog(summary),
            StandardCharsets.UTF_8
        )
    }

    fun logFilePath(): Path = logFile

    fun appendWarning(taskId: String, message: String) {
        Files.createDirectories(logDirectory)
        Files.writeString(
            logFile,
            "\n> Security warning for $taskId:\n> ${message.replace("\n", "\n> ")}\n",
            StandardCharsets.UTF_8,
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.APPEND
        )
    }

    private fun renderAttempts(): String = buildString {
        appendLine("# Execution Loop Run $runId")
        appendLine()
        appendLine("## Task Attempts")
        appendLine()
        attempts.forEachIndexed { index, attempt ->
            append(renderAttempt(attempt, index + 1))
            appendLine()
        }
    }

    private fun renderFullLog(summary: ExecutionRunSummary): String = buildString {
        appendLine("# Execution Loop Run ${summary.runId}")
        appendLine()
        appendLine("## Summary")
        appendLine()
        appendLine("- Run number: ${summary.runNumber}")
        appendLine("- Model: ${summary.model}")
        appendLine("- Started: ${formatInstant(summary.startedAt)}")
        appendLine("- Finished: ${formatInstant(summary.finishedAt)}")
        appendLine("- Duration: ${formatDuration(Duration.between(summary.startedAt, summary.finishedAt))}")
        appendLine("- Uninterrupted duration: ${"%.2f".format(summary.uninterruptedDurationMinutes)} minutes")
        appendLine("- Total tasks: ${summary.totalTasks}")
        appendLine("- Completed tasks: ${summary.completedTasks}")
        appendLine("- Consecutive tasks without intervention: ${summary.consecutiveTasksWithoutIntervention}")
        appendLine("- Failed tasks: ${summary.failedTasks}")
        appendLine("- Average task duration: ${formatDuration(summary.averageTaskDuration)}")
        appendLine("- First-pass success rate: ${"%.1f".format(summary.firstPassSuccessRate * 100)}%")
        appendLine("- Stopped early: ${summary.stoppedEarly}")
        summary.breakTaskId?.let { appendLine("- Break task: $it") }
        if (summary.breakFailureCategory != ExecutionFailureCategory.NONE) {
            appendLine("- Break category: ${summary.breakFailureCategory}")
        }
        summary.stopReason?.let { appendLine("- Stop reason: $it") }
        appendLine()
        appendLine("## Task Attempts")
        appendLine()
        summary.attempts.forEachIndexed { index, attempt ->
            append(renderAttempt(attempt, index + 1))
            appendLine()
        }
    }

    private fun renderAttempt(
        attempt: ExecutionAttemptRecord,
        index: Int
    ): String = buildString {
        appendLine("### Attempt $index — ${attempt.taskId}")
        appendLine()
        appendLine("- Task: ${attempt.taskDescription}")
        appendLine("- Type: ${attempt.taskType}")
        appendLine("- Profile: ${attempt.profile}")
        appendLine("- Started: ${formatInstant(attempt.startedAt)}")
        appendLine("- Attempt number: ${attempt.attemptNumber}")
        appendLine("- Status: ${attempt.status}")
        appendLine("- Failure category: ${attempt.failureCategory}")
        appendLine("- Duration: ${formatDuration(attempt.duration)}")
        attempt.commitResult?.let { appendLine("- Commit: $it") }
        appendLine()
        appendLine("#### Agent result")
        appendLine()
        appendLine("```text")
        appendLine(attempt.agentResult.trim().ifBlank { "<empty>" })
        appendLine("```")
        appendLine()
        appendLine("#### Validation result")
        appendLine()
        appendLine("```text")
        appendLine(attempt.validationResult.trim().ifBlank { "<empty>" })
        appendLine("```")
        attempt.securityResult?.let { security ->
            appendLine()
            appendLine("#### Security review")
            appendLine()
            appendLine("```text")
            appendLine(security.trim())
            appendLine("```")
        }
        attempt.gatewayResult?.let { gateway ->
            appendLine()
            appendLine("#### Gateway audit")
            appendLine()
            appendLine("```text")
            appendLine(gateway.trim())
            appendLine("```")
        }
    }

    private fun formatInstant(instant: Instant): String =
        FORMATTER.format(instant.atZone(ZONE))

    private fun formatDuration(duration: Duration): String {
        val seconds = duration.seconds
        val millis = duration.toMillisPart()
        return "${seconds}.${millis.toString().padStart(3, '0')}s"
    }

    private companion object {
        val ZONE: ZoneId = ZoneId.systemDefault()
        val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
