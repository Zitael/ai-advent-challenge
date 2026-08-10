package ru.maleks.ai_advent_challenge_app.executionloop

import ru.maleks.ai_advent_challenge_app.developer.profile.AssistantProfile
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

enum class ExecutionTaskType {
    BUG_FIX,
    FEATURE,
    REFACTOR,
    TEST,
    DOCUMENTATION,
    RESEARCH;

    companion object {
        fun fromRaw(value: String?): ExecutionTaskType = when (value?.trim()?.lowercase()) {
            "bug-fix", "bugfix", "bug_fix" -> BUG_FIX
            "feature" -> FEATURE
            "refactor", "refactoring" -> REFACTOR
            "test", "tests" -> TEST
            "research" -> RESEARCH
            else -> DOCUMENTATION
        }
    }
}

enum class ExecutionTaskProfile {
    BUG_FIX,
    RESEARCH,
    ARCHITECTURE,
    DOCUMENTATION;

    fun instruction(): String = when (this) {
        BUG_FIX -> AssistantProfile.BugFix.instruction
        RESEARCH -> AssistantProfile.Research.instruction
        ARCHITECTURE -> AssistantProfile.Architecture.instruction
        DOCUMENTATION -> DOCUMENTATION_PROFILE_INSTRUCTION
    }

    companion object {
        fun fromRaw(value: String?): ExecutionTaskProfile {
            return when (value?.trim()?.lowercase()) {
                "bug-fix", "bugfix", "bug_fix" -> BUG_FIX
                "research" -> RESEARCH
                "architecture", "feature", "refactor" -> ARCHITECTURE
                "documentation", "docs", "doc" -> DOCUMENTATION
                else -> DOCUMENTATION
            }
        }
    }
}

private val DOCUMENTATION_PROFILE_INSTRUCTION = """
    You are working in DOCUMENTATION profile.

    Goal: produce accurate project documentation based on real files.
    Do not change production code unless the task explicitly requires it.
    Prefer writing Markdown under execution-loop/artifacts/.
    Cite exact file paths from the repository.
""".trimIndent()

data class ExecutionTask(
    val id: String,
    val description: String,
    val type: ExecutionTaskType,
    val profile: ExecutionTaskProfile,
    val outputPath: Path?,
    val validation: ExecutionValidationKind,
    val expectedContent: String? = null,
    val commitMessage: String? = null
)

enum class ExecutionValidationKind {
    FILE_EXISTS,
    FILE_CONTAINS,
    INVARIANTS,
    GRADLE_TEST
}

enum class ExecutionTaskStatus {
    SUCCESS,
    FAILED
}

enum class ExecutionFailureCategory {
    NONE,
    NO_FILE_CHANGES,
    VALIDATION_FAILED,
    INVARIANTS,
    GRADLE_TEST,
    LLM_ERROR,
    GIT_COMMIT,
    SECURITY_REVIEW,
    GATEWAY_BLOCKED
}

data class ExecutionAttemptRecord(
    val taskId: String,
    val taskDescription: String,
    val taskType: ExecutionTaskType,
    val profile: ExecutionTaskProfile,
    val startedAt: Instant,
    val attemptNumber: Int,
    val agentResult: String,
    val validationResult: String,
    val securityResult: String? = null,
    val gatewayResult: String? = null,
    val commitResult: String?,
    val status: ExecutionTaskStatus,
    val failureCategory: ExecutionFailureCategory,
    val duration: Duration
)

data class ExecutionTaskMetric(
    val taskId: String,
    val succeeded: Boolean,
    val attemptsUsed: Int,
    val firstPassSuccess: Boolean,
    val duration: Duration,
    val failureCategory: ExecutionFailureCategory,
    val failureReason: String?
)

data class ExecutionRunSummary(
    val runId: String,
    val runNumber: Int,
    val model: String,
    val startedAt: Instant,
    val finishedAt: Instant,
    val totalTasks: Int,
    val completedTasks: Int,
    val failedTasks: Int,
    val consecutiveTasksWithoutIntervention: Int,
    val stoppedEarly: Boolean,
    val stopReason: String?,
    val breakTaskId: String?,
    val breakFailureCategory: ExecutionFailureCategory,
    val averageTaskDuration: Duration,
    val firstPassSuccessRate: Double,
    val uninterruptedDurationMinutes: Double,
    val attempts: List<ExecutionAttemptRecord>,
    val taskMetrics: List<ExecutionTaskMetric>
)
