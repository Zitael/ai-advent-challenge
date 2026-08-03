package ru.maleks.ai_advent_challenge_app.micromodel

import ru.maleks.ai_advent_challenge_app.dataset.TicketCategory

enum class MicroModelStatus {
    OK,
    UNSURE
}

enum class MicromodelTestKind {
    SIMPLE,
    EDGE,
    COMPLEX
}

data class MicroClassificationResult(
    val category: String?,
    val confidence: Double,
    val status: MicroModelStatus,
    val matchedSignals: List<String>,
    val latencyMs: Long
)

data class MicromodelTestCase(
    val id: String,
    val ticketText: String,
    val expectedCategory: String? = null,
    val kind: MicromodelTestKind
)

data class MicromodelOutcome(
    val testCaseId: String,
    val kind: MicromodelTestKind,
    val handledByMicro: Boolean,
    val fallbackUsed: Boolean,
    val microStatus: MicroModelStatus,
    val microConfidence: Double,
    val microCategory: String?,
    val finalCategory: String?,
    val expectedCategory: String?,
    val correct: Boolean?,
    val fallbackReasons: List<String>,
    val microLatencyMs: Long,
    val fallbackLatencyMs: Long,
    val totalLatencyMs: Long,
    val fallbackTokens: Int,
    val fallbackCost: Double?
)

data class MicromodelEvaluationReport(
    val microModelName: String,
    val fallbackModel: String,
    val confidenceThreshold: Double,
    val totalCases: Int,
    val microHandledCount: Int,
    val fallbackCount: Int,
    val llmCallCount: Int,
    val microCorrectCount: Int,
    val fallbackCorrectCount: Int,
    val measurableCases: Int,
    val avgLatencyMs: Long,
    val microAvgLatencyMs: Long,
    val fallbackAvgLatencyMs: Long,
    val totalFallbackTokens: Int,
    val totalFallbackCost: Double?,
    val estimatedAlwaysLlmLatencyMs: Long,
    val latencySavedMs: Long,
    val outcomes: List<MicromodelOutcome>
)
