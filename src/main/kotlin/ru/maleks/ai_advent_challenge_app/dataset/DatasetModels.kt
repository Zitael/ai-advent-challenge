package ru.maleks.ai_advent_challenge_app.dataset

enum class TicketCategory(val label: String) {
    BILLING("billing"),
    ACCOUNT("account"),
    TECHNICAL("technical"),
    FEATURE_REQUEST("feature_request");

    companion object {
        val labels: Set<String> = entries.map { it.label }.toSet()

        fun fromLabel(value: String): TicketCategory? =
            entries.firstOrNull { it.label == value.trim().lowercase() }
    }
}

data class FineTuningMessage(
    val role: String,
    val content: String
)

data class FineTuningExample(
    val messages: List<FineTuningMessage>,
    val source: String = "unknown",
    val real: Boolean = false
)

data class RawTicketExample(
    val id: String,
    val source: String,
    val userMessage: String,
    val category: String
)

data class DatasetBuildReport(
    val totalRaw: Int,
    val afterCleaning: Int,
    val realCount: Int,
    val syntheticCount: Int,
    val trainCount: Int,
    val evalCount: Int,
    val trainPath: String,
    val evalPath: String
)

data class ValidationIssue(
    val lineNumber: Int,
    val message: String
)

data class ValidationReport(
    val filePath: String,
    val totalLines: Int,
    val validLines: Int,
    val issues: List<ValidationIssue>
) {
    val passed: Boolean
        get() = issues.isEmpty() && validLines == totalLines && totalLines > 0
}

data class BaselineSampleResult(
    val index: Int,
    val userMessage: String,
    val expectedCategory: String,
    val modelAnswer: String,
    val exactMatch: Boolean,
    val formatValid: Boolean
)

data class BaselineReport(
    val model: String,
    val sampleCount: Int,
    val exactMatches: Int,
    val formatValidCount: Int,
    val samples: List<BaselineSampleResult>
)
