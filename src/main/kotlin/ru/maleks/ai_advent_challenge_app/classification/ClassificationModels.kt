package ru.maleks.ai_advent_challenge_app.classification

import ru.maleks.ai_advent_challenge_app.dataset.TicketCategory

enum class ConfidenceStatus {
    OK,
    UNSURE,
    FAIL,
    REJECTED
}

enum class TestCaseKind {
    CORRECT,
    EDGE,
    NOISY
}

data class ClassificationTestCase(
    val id: String,
    val ticketText: String,
    val expectedCategory: String? = null,
    val kind: TestCaseKind
)

data class LlmCallMetrics(
    val latencyMs: Long,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val cost: Double?
)

data class ConstraintCheckResult(
    val passed: Boolean,
    val category: String?,
    val details: String
)

data class RedundancyCheckResult(
    val passed: Boolean,
    val consensusCategory: String?,
    val votes: List<String>,
    val agreementRatio: Double,
    val details: String,
    val metrics: LlmCallMetrics
)

data class SelfCheckResult(
    val passed: Boolean,
    val verifiedCategory: String?,
    val rawAnswer: String,
    val details: String,
    val metrics: LlmCallMetrics
)

data class ScoringResult(
    val passed: Boolean,
    val category: String?,
    val confidence: Double?,
    val status: ConfidenceStatus?,
    val rawAnswer: String,
    val details: String,
    val metrics: LlmCallMetrics
)

data class ApproachOutcome(
    val name: String,
    val passed: Boolean,
    val details: String
)

data class ClassificationOutcome(
    val testCaseId: String,
    val kind: TestCaseKind,
    val acceptedCategory: String?,
    val finalStatus: ConfidenceStatus,
    val expectedCategory: String?,
    val correct: Boolean?,
    val candidateCategory: String?,
    val approaches: List<ApproachOutcome>,
    val inferenceCalls: Int,
    val rejected: Boolean,
    val retried: Boolean,
    val totalLatencyMs: Long,
    val totalTokens: Int,
    val totalCost: Double?
)

data class ConfidenceEvaluationReport(
    val model: String,
    val totalCases: Int,
    val acceptedCount: Int,
    val rejectedCount: Int,
    val unsureCount: Int,
    val failCount: Int,
    val retryCount: Int,
    val correctAcceptedCount: Int,
    val measurableCases: Int,
    val baselineLatencyMs: Long,
    val confidenceLatencyMs: Long,
    val baselineTokens: Int,
    val confidenceTokens: Int,
    val baselineCost: Double?,
    val confidenceCost: Double?,
    val outcomes: List<ClassificationOutcome>
)

object ClassificationAnswerParser {
    fun normalize(raw: String): String =
        raw.trim()
            .lineSequence()
            .first()
            .trim()
            .lowercase()
            .removeSuffix(".")
            .removeSuffix(",")
            .removePrefix("\"")
            .removeSuffix("\"")

    fun parseCategory(raw: String): String? =
        TicketCategory.fromLabel(normalize(raw))?.label
}
