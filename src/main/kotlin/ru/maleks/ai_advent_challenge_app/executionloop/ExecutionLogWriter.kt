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
        appendLine("- Started: ${formatInstant(summary.startedAt)}")
        appendLine("- Finished: ${formatInstant(summary.finishedAt)}")
        appendLine("- Duration: ${formatDuration(Duration.between(summary.startedAt, summary.finishedAt))}")
        appendLine("- Total tasks: ${summary.totalTasks}")
        appendLine("- Completed tasks: ${summary.completedTasks}")
        appendLine("- Failed tasks: ${summary.failedTasks}")
        appendLine("- Stopped early: ${summary.stoppedEarly}")
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
        appendLine("- Started: ${formatInstant(attempt.startedAt)}")
        appendLine("- Attempt number: ${attempt.attemptNumber}")
        appendLine("- Status: ${attempt.status}")
        appendLine("- Duration: ${formatDuration(attempt.duration)}")
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
