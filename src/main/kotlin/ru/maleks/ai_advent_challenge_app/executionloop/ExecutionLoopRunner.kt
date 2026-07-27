package ru.maleks.ai_advent_challenge_app.executionloop

import java.time.Duration
import java.time.Instant

class ExecutionLoopRunner(
    private val config: ExecutionLoopConfig,
    private val queueLoader: ExecutionTaskQueueLoader,
    private val taskExecutor: ExecutionTaskExecutor,
    private val taskValidator: ExecutionTaskValidator,
    private val logWriterFactory: (String) -> ExecutionLogWriter
) {

    suspend fun run(): ExecutionRunSummary {
        val runId = Instant.now().toString().replace(':', '-')
        val startedAt = Instant.now()
        val logWriter = logWriterFactory(runId)

        val allTasks = queueLoader.load(config.queueFile)
        val tasks = config.taskLimit?.let { limit -> allTasks.take(limit) } ?: allTasks

        val attempts = mutableListOf<ExecutionAttemptRecord>()
        var completedTasks = 0
        var failedTasks = 0
        var stoppedEarly = false
        var stopReason: String? = null

        for (task in tasks) {
            var previousFailure: String? = null
            var taskSucceeded = false

            for (attemptNumber in 1..config.maxAttemptsPerTask) {
                val attemptStartedAt = Instant.now()
                val agentResult = taskExecutor.execute(
                    task = task,
                    attemptNumber = attemptNumber,
                    projectRoot = config.projectRoot,
                    previousFailure = previousFailure
                )
                val validationResult = taskValidator.validate(task, agentResult)
                val attemptFinishedAt = Instant.now()

                val status = if (validationResult.passed) {
                    ExecutionTaskStatus.SUCCESS
                } else {
                    ExecutionTaskStatus.FAILED
                }

                val record = ExecutionAttemptRecord(
                    taskId = task.id,
                    taskDescription = task.description,
                    startedAt = attemptStartedAt,
                    attemptNumber = attemptNumber,
                    agentResult = agentResult.agentSummary,
                    validationResult = validationResult.message,
                    status = status,
                    duration = Duration.between(attemptStartedAt, attemptFinishedAt)
                )

                attempts += record
                logWriter.recordAttempt(record)

                if (validationResult.passed) {
                    taskSucceeded = true
                    completedTasks++
                    break
                }

                previousFailure = validationResult.message
            }

            if (!taskSucceeded) {
                failedTasks++
                stoppedEarly = true
                stopReason = "Task ${task.id} failed after ${config.maxAttemptsPerTask} attempts."
                break
            }
        }

        val finishedAt = Instant.now()
        val summary = ExecutionRunSummary(
            runId = runId,
            startedAt = startedAt,
            finishedAt = finishedAt,
            totalTasks = tasks.size,
            completedTasks = completedTasks,
            failedTasks = failedTasks,
            stoppedEarly = stoppedEarly,
            stopReason = stopReason,
            attempts = attempts
        )

        logWriter.writeSummary(summary)
        return summary
    }
}
