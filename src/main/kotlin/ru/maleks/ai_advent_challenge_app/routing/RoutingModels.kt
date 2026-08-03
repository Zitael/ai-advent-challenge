package ru.maleks.ai_advent_challenge_app.routing

enum class RoutedModel {
    CHEAP,
    STRONG
}

enum class RoutingTestKind {
    SIMPLE,
    EDGE,
    NOISY
}

data class RoutingTestCase(
    val id: String,
    val ticketText: String,
    val expectedCategory: String? = null,
    val kind: RoutingTestKind,
    val expectEscalation: Boolean? = null
)

data class HeuristicCheck(
    val name: String,
    val passed: Boolean,
    val details: String
)

data class HeuristicEvaluation(
    val shouldEscalate: Boolean,
    val checks: List<HeuristicCheck>
)

data class RoutingOutcome(
    val testCaseId: String,
    val kind: RoutingTestKind,
    val routedModel: RoutedModel,
    val escalated: Boolean,
    val escalationReasons: List<String>,
    val acceptedCategory: String?,
    val expectedCategory: String?,
    val correct: Boolean?,
    val cheapConfidence: Double?,
    val cheapStatus: String?,
    val heuristics: List<HeuristicCheck>,
    val cheapLatencyMs: Long,
    val strongLatencyMs: Long,
    val totalLatencyMs: Long,
    val cheapTokens: Int,
    val strongTokens: Int,
    val totalTokens: Int,
    val cheapCost: Double?,
    val strongCost: Double?,
    val totalCost: Double?
)

data class RoutingEvaluationReport(
    val cheapModel: String,
    val strongModel: String,
    val totalCases: Int,
    val cheapOnlyCount: Int,
    val escalatedCount: Int,
    val correctCount: Int,
    val measurableCases: Int,
    val cheapOnlyLatencyMs: Long,
    val escalatedLatencyMs: Long,
    val totalLatencyMs: Long,
    val cheapOnlyTokens: Int,
    val escalatedTokens: Int,
    val totalTokens: Int,
    val cheapOnlyCost: Double?,
    val escalatedCost: Double?,
    val totalCost: Double?,
    val outcomes: List<RoutingOutcome>
)
