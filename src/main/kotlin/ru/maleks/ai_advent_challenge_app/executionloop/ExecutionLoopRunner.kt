package ru.maleks.ai_advent_challenge_app.executionloop

import java.time.Duration
import java.time.Instant

class ExecutionLoopRunner(
    private val config: ExecutionLoopConfig,
    private val queueLoader: ExecutionTaskQueueLoader,
    private val taskExecutor: ExecutionTaskExecutor,
    private val taskValidator: ExecutionTaskValidator,
    private val gitCommitter: ExecutionGitCommitter,
    private val logWriterFactory: (String) -> ExecutionLogWriter,
    private val metricsWriter: ExecutionMetricsWriter
) {

    suspend fun run(): ExecutionRunSummary {
        val runId = Instant.now().toString().replace(':', '-')
        val startedAt = Instant.now()
        val logWriter = logWriterFactory(runId)

        val allTasks = queueLoader.load(config.queueFile)
        val tasks = config.taskLimit?.let { limit -> allTasks.take(limit) } ?: allTasks

        val attempts = mutableListOf<ExecutionAttemptRecord>()
        val taskMetrics = mutableListOf<ExecutionTaskMetric>()
        var completedTasks = 0
        var failedTasks = 0
        var stoppedEarly = false
        var stopReason: String? = null
        var breakTaskId: String? = null
        var breakFailureCategory = ExecutionFailureCategory.NONE
        var firstPassSuccesses = 0
        var attemptedTasks = 0

        for (task in tasks) {
            val taskStartedAt = Instant.now()
            var previousFailure: String? = null
            var taskSucceeded = false
            var attemptsUsed = 0
            var lastFailureCategory = ExecutionFailureCategory.NONE
            var lastFailureReason: String? = null

            for (attemptNumber in 1..config.maxAttemptsPerTask) {
                attemptsUsed = attemptNumber
                val attemptStartedAt = Instant.now()

                val agentResult = try {
                    taskExecutor.execute(
                        task = task,
                        attemptNumber = attemptNumber,
                        projectRoot = config.projectRoot,
                        previousFailure = previousFailure
                    )
                } catch (exception: Exception) {
                    val record = ExecutionAttemptRecord(
                        taskId = task.id,
                        taskDescription = task.description,
                        taskType = task.type,
                        profile = task.profile,
                        startedAt = attemptStartedAt,
                        attemptNumber = attemptNumber,
                        agentResult = "LLM error: ${exception.message}",
                        validationResult = "Execution aborted before validation.",
                        commitResult = null,
                        status = ExecutionTaskStatus.FAILED,
                        failureCategory = ExecutionFailureCategory.LLM_ERROR,
                        duration = Duration.between(attemptStartedAt, Instant.now())
                    )
                    attempts += record
                    logWriter.recordAttempt(record)
                    lastFailureCategory = ExecutionFailureCategory.LLM_ERROR
                    lastFailureReason = exception.message
                    previousFailure = exception.message
                    continue
                }

                val validationResult = taskValidator.validate(task, agentResult)
                val attemptFinishedAt = Instant.now()
                val status = if (validationResult.passed) {
                    ExecutionTaskStatus.SUCCESS
                } else {
                    ExecutionTaskStatus.FAILED
                }

                var commitResult: String? = null
                if (validationResult.passed && config.createCommits) {
                    val commitMessage = task.commitMessage ?: "execution-loop: ${task.id}"
                    commitResult = gitCommitter.commit(
                        files = agentResult.applyResult.appliedFiles,
                        message = commitMessage
                    ).message
                }

                val record = ExecutionAttemptRecord(
                    taskId = task.id,
                    taskDescription = task.description,
                    taskType = task.type,
                    profile = task.profile,
                    startedAt = attemptStartedAt,
                    attemptNumber = attemptNumber,
                    agentResult = agentResult.agentSummary,
                    validationResult = validationResult.message,
                    commitResult = commitResult,
                    status = status,
                    failureCategory = if (validationResult.passed) {
                        ExecutionFailureCategory.NONE
                    } else {
                        validationResult.category
                    },
                    duration = Duration.between(attemptStartedAt, attemptFinishedAt)
                )

                attempts += record
                logWriter.recordAttempt(record)

                if (validationResult.passed) {
                    taskSucceeded = true
                    completedTasks++
                    if (attemptNumber == 1) {
                        firstPassSuccesses++
                    }
                    break
                }

                lastFailureCategory = validationResult.category
                lastFailureReason = validationResult.message
                previousFailure = validationResult.message
            }

            attemptedTasks++
            taskMetrics += ExecutionTaskMetric(
                taskId = task.id,
                succeeded = taskSucceeded,
                attemptsUsed = attemptsUsed,
                firstPassSuccess = taskSucceeded && attemptsUsed == 1,
                duration = Duration.between(taskStartedAt, Instant.now()),
                failureCategory = if (taskSucceeded) ExecutionFailureCategory.NONE else lastFailureCategory,
                failureReason = if (taskSucceeded) null else lastFailureReason
            )

            if (!taskSucceeded) {
                failedTasks++
                stoppedEarly = true
                breakTaskId = task.id
                breakFailureCategory = lastFailureCategory
                stopReason = "Task ${task.id} failed after ${config.maxAttemptsPerTask} attempts: $lastFailureReason"
                break
            }
        }

        val finishedAt = Instant.now()
        val averageTaskDuration = if (taskMetrics.isEmpty()) {
            Duration.ZERO
        } else {
            Duration.ofMillis(
                taskMetrics.map { it.duration.toMillis() }.average().toLong()
            )
        }

        val summary = ExecutionRunSummary(
            runId = runId,
            runNumber = config.runNumber,
            model = config.ollamaModel,
            startedAt = startedAt,
            finishedAt = finishedAt,
            totalTasks = tasks.size,
            completedTasks = completedTasks,
            failedTasks = failedTasks,
            consecutiveTasksWithoutIntervention = completedTasks,
            stoppedEarly = stoppedEarly,
            stopReason = stopReason,
            breakTaskId = breakTaskId,
            breakFailureCategory = breakFailureCategory,
            averageTaskDuration = averageTaskDuration,
            firstPassSuccessRate = if (attemptedTasks == 0) {
                0.0
            } else {
                firstPassSuccesses.toDouble() / attemptedTasks.toDouble()
            },
            uninterruptedDurationMinutes = Duration.between(startedAt, finishedAt).seconds / 60.0,
            attempts = attempts,
            taskMetrics = taskMetrics
        )

        logWriter.writeSummary(summary)
        metricsWriter.write(summary)
        return summary
    }
}
