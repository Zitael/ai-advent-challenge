package ru.maleks.ai_advent_challenge_app.decomposition

enum class InferenceVariant {
    MONOLITHIC,
    MULTI_STAGE
}

enum class TicketIntent {
    BILLING,
    ACCOUNT,
    TECHNICAL,
    FEATURE,
    MIXED;

    companion object {
        fun fromRaw(value: String?): TicketIntent? =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) }
    }
}

enum class TicketPriority(val label: String) {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    URGENT("urgent");

    companion object {
        fun fromLabel(value: String?): TicketPriority? =
            entries.firstOrNull { it.label == value?.trim()?.lowercase() }
    }
}

enum class TicketAction(val label: String) {
    AUTO_REPLY("auto_reply"),
    ESCALATE("escalate"),
    HUMAN_REVIEW("human_review");

    companion object {
        fun fromLabel(value: String?): TicketAction? =
            entries.firstOrNull { it.label == value?.trim()?.lowercase() }
    }
}

data class StageMetrics(
    val stageName: String,
    val model: String,
    val latencyMs: Long,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val cost: Double?
)

data class NormalizedTicket(
    val intent: TicketIntent,
    val signals: List<String>,
    val cleanText: String
)

data class TriageDecision(
    val category: String,
    val priority: TicketPriority,
    val action: TicketAction
)

data class TriageResult(
    val category: String,
    val priority: TicketPriority,
    val action: TicketAction,
    val summary: String
) {
    fun formatValid(): Boolean =
        ru.maleks.ai_advent_challenge_app.dataset.TicketCategory.fromLabel(category) != null &&
            summary.isNotBlank() &&
            summary.length <= 120
}

data class TriageRunOutcome(
    val testCaseId: String,
    val variant: InferenceVariant,
    val result: TriageResult?,
    val formatValid: Boolean,
    val matchedFields: Int,
    val expectedFields: Int,
    val stageMetrics: List<StageMetrics>,
    val totalLatencyMs: Long,
    val totalTokens: Int,
    val totalCost: Double?,
    val rawStages: List<String>
)

data class DecompositionTestCase(
    val id: String,
    val ticketText: String,
    val expectedCategory: String? = null,
    val expectedPriority: String? = null,
    val expectedAction: String? = null
)

data class DecompositionEvaluationReport(
    val monolithicModel: String,
    val analyzeModel: String,
    val decideModel: String,
    val formatModel: String,
    val totalCases: Int,
    val monolithicValidCount: Int,
    val multiStageValidCount: Int,
    val monolithicMatchedFields: Int,
    val multiStageMatchedFields: Int,
    val expectedFieldChecks: Int,
    val monolithicLatencyMs: Long,
    val multiStageLatencyMs: Long,
    val monolithicTokens: Int,
    val multiStageTokens: Int,
    val monolithicCost: Double?,
    val multiStageCost: Double?,
    val outcomes: List<TriageRunOutcome>
)
