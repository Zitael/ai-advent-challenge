package ru.maleks.ai_advent_challenge_app.executionloop

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

class ExecutionMetricsWriter(
    private val metricsDirectory: Path
) {
    fun write(summary: ExecutionRunSummary) {
        Files.createDirectories(metricsDirectory)
        val file = metricsDirectory.resolve("run-${summary.runNumber}-metrics.md")
        Files.writeString(file, render(summary), StandardCharsets.UTF_8)
    }

    private fun render(summary: ExecutionRunSummary): String = buildString {
        appendLine("# Execution Loop Metrics — Run ${summary.runNumber}")
        appendLine()
        appendLine("- Model: `${summary.model}`")
        appendLine("- Run id: `${summary.runId}`")
        appendLine("- Completed tasks: ${summary.completedTasks}/${summary.totalTasks}")
        appendLine("- Consecutive tasks without intervention: ${summary.consecutiveTasksWithoutIntervention}")
        appendLine("- Uninterrupted duration: ${"%.2f".format(summary.uninterruptedDurationMinutes)} minutes")
        appendLine("- Average task duration: ${formatDuration(summary.averageTaskDuration)}")
        appendLine("- First-pass success rate: ${"%.1f".format(summary.firstPassSuccessRate * 100)}%")
        appendLine("- Stopped early: ${summary.stoppedEarly}")
        summary.breakTaskId?.let { appendLine("- Broke on task: `$it`") }
        if (summary.breakFailureCategory != ExecutionFailureCategory.NONE) {
            appendLine("- Break category: ${summary.breakFailureCategory}")
        }
        summary.stopReason?.let { appendLine("- Break reason: $it") }
        appendLine()
        appendLine("## Task Results")
        appendLine()
        appendLine("| Task | Status | Attempts | First pass | Duration | Failure |")
        appendLine("|---|---|---:|---:|---:|---|")
        summary.taskMetrics.forEach { metric ->
            appendLine(
                "| ${metric.taskId} | ${if (metric.succeeded) "DONE" else "FAILED"} | " +
                    "${metric.attemptsUsed} | ${if (metric.firstPassSuccess) "yes" else "no"} | " +
                    "${formatDuration(metric.duration)} | ${metric.failureCategory} |"
            )
        }
    }

    private fun formatDuration(duration: Duration): String {
        val seconds = duration.seconds
        val millis = duration.toMillisPart()
        return "${seconds}.${millis.toString().padStart(3, '0')}s"
    }
}
