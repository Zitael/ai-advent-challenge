package ru.maleks.ai_advent_challenge_app.executionloop

import java.time.Duration
import java.time.Instant

class ExecutionLoopRunner(
    private val config: ExecutionLoopConfig,
    private val queueLoader: ExecutionTaskQueueLoader,
    private val taskExecutor: ExecutionTaskExecutor,
    private val taskValidator: ExecutionTaskValidator,
    private val securityReviewer: ExecutionSecurityReviewer?,
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

                if (agentResult.gatewayResult?.blocked == true) {
                    val gatewayMessage = agentResult.applyResult.message
                    val record = ExecutionAttemptRecord(
                        taskId = task.id,
                        taskDescription = task.description,
                        taskType = task.type,
                        profile = task.profile,
                        startedAt = attemptStartedAt,
                        attemptNumber = attemptNumber,
                        agentResult = agentResult.agentSummary,
                        validationResult = "Skipped: gateway blocked generation.",
                        gatewayResult = renderGatewayLog(agentResult.gatewayResult),
                        commitResult = null,
                        status = ExecutionTaskStatus.FAILED,
                        failureCategory = ExecutionFailureCategory.GATEWAY_BLOCKED,
                        duration = Duration.between(attemptStartedAt, Instant.now())
                    )
                    attempts += record
                    logWriter.recordAttempt(record)
                    lastFailureCategory = ExecutionFailureCategory.GATEWAY_BLOCKED
                    lastFailureReason = gatewayMessage
                    previousFailure = gatewayMessage
                    continue
                }

                val validationResult = taskValidator.validate(task, agentResult)
                if (!validationResult.passed) {
                    val record = buildAttemptRecord(
                        task = task,
                        attemptStartedAt = attemptStartedAt,
                        attemptNumber = attemptNumber,
                        agentResult = agentResult,
                        validationResult = validationResult.message,
                        securityResult = null,
                        gatewayResult = renderGatewayLog(agentResult.gatewayResult),
                        commitResult = null,
                        status = ExecutionTaskStatus.FAILED,
                        failureCategory = validationResult.category
                    )
                    attempts += record
                    logWriter.recordAttempt(record)
                    lastFailureCategory = validationResult.category
                    lastFailureReason = validationResult.message
                    previousFailure = validationResult.message
                    continue
                }

                val securityResult = if (config.securityReviewEnabled && securityReviewer != null) {
                    securityReviewer.review(agentResult, task)
                } else {
                    null
                }

                if (securityResult?.decision == SecurityReviewDecision.BLOCK) {
                    val record = buildAttemptRecord(
                        task = task,
                        attemptStartedAt = attemptStartedAt,
                        attemptNumber = attemptNumber,
                        agentResult = agentResult,
                        validationResult = validationResult.message,
                        securityResult = renderSecurityLog(securityResult),
                        gatewayResult = renderGatewayLog(
                            agentResult.gatewayResult,
                            securityResult.gatewayCalls
                        ),
                        commitResult = null,
                        status = ExecutionTaskStatus.FAILED,
                        failureCategory = ExecutionFailureCategory.SECURITY_REVIEW
                    )
                    attempts += record
                    logWriter.recordAttempt(record)
                    lastFailureCategory = ExecutionFailureCategory.SECURITY_REVIEW
                    lastFailureReason = securityResult.feedback
                    previousFailure = securityResult.feedback
                    continue
                }

                var commitResult: String? = null
                if (config.createCommits) {
                    val commitMessage = task.commitMessage ?: "execution-loop: ${task.id}"
                    commitResult = gitCommitter.commit(
                        files = agentResult.applyResult.appliedFiles,
                        message = commitMessage
                    ).message
                }

                val securityLog = securityResult?.let { renderSecurityLog(it) }
                if (securityResult?.decision == SecurityReviewDecision.PASS_WITH_WARNINGS) {
                    securityLog?.let { logWriter.appendWarning(task.id, it) }
                }

                val record = buildAttemptRecord(
                    task = task,
                    attemptStartedAt = attemptStartedAt,
                    attemptNumber = attemptNumber,
                    agentResult = agentResult,
                    validationResult = validationResult.message,
                    securityResult = securityLog,
                    gatewayResult = renderGatewayLog(
                        agentResult.gatewayResult,
                        securityResult?.gatewayCalls.orEmpty()
                    ),
                    commitResult = commitResult,
                    status = ExecutionTaskStatus.SUCCESS,
                    failureCategory = ExecutionFailureCategory.NONE
                )

                attempts += record
                logWriter.recordAttempt(record)
                taskSucceeded = true
                completedTasks++
                if (attemptNumber == 1) {
                    firstPassSuccesses++
                }
                break
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
            model = config.gatewayModel,
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

    private fun buildAttemptRecord(
        task: ExecutionTask,
        attemptStartedAt: Instant,
        attemptNumber: Int,
        agentResult: ExecutionAgentResult,
        validationResult: String,
        securityResult: String?,
        gatewayResult: String?,
        commitResult: String?,
        status: ExecutionTaskStatus,
        failureCategory: ExecutionFailureCategory
    ): ExecutionAttemptRecord = ExecutionAttemptRecord(
        taskId = task.id,
        taskDescription = task.description,
        taskType = task.type,
        profile = task.profile,
        startedAt = attemptStartedAt,
        attemptNumber = attemptNumber,
        agentResult = agentResult.agentSummary,
        validationResult = validationResult,
        securityResult = securityResult,
        gatewayResult = gatewayResult,
        commitResult = commitResult,
        status = status,
        failureCategory = failureCategory,
        duration = Duration.between(attemptStartedAt, Instant.now())
    )

    private fun renderSecurityLog(result: ExecutionSecurityReviewResult): String = buildString {
        appendLine("Decision: ${result.decision}")
        appendLine("Summary: ${result.summary}")
        result.findings.forEach { finding ->
            appendLine("- [${finding.severity}] ${finding.category}: ${finding.message} (${finding.source})")
        }
    }.trim()

    private fun renderGatewayLog(
        generation: ExecutionGatewayResult?,
        reviewCalls: List<GatewayCallLog> = emptyList()
    ): String? {
        val parts = mutableListOf<String>()
        generation?.toGatewayCallLog()?.let { parts += renderSingleGatewayCall(it) }
        reviewCalls.forEach { parts += renderSingleGatewayCall(it) }
        return parts.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    private fun renderSingleGatewayCall(call: GatewayCallLog): String = buildString {
        append("purpose=${call.purpose}, action=${call.inputGuardAction}, blocked=${call.blocked}")
        if (call.inputFindings.isNotEmpty()) {
            append(", inputFindings=${call.inputFindings}")
        }
        if (call.outputViolations.isNotEmpty()) {
            append(", outputViolations=${call.outputViolations}")
        }
    }
}
