package ru.maleks.ai_advent_challenge_app.decomposition

data class MultiStageModelConfig(
    val analyzeModel: String,
    val decideModel: String,
    val formatModel: String
)

class MultiStageTriagePipeline(
    private val stageExecutor: StageExecutor,
    private val parser: CompactToonParser = CompactToonParser(),
    private val models: MultiStageModelConfig
) {

    suspend fun triage(testCase: DecompositionTestCase): TriageRunOutcome {
        val analyzeStage = stageExecutor.runStage(
            stageName = "analyze",
            model = models.analyzeModel,
            systemPrompt = DecompositionPrompts.ANALYZE_SYSTEM,
            userPrompt = DecompositionPrompts.analyzeUser(testCase.ticketText),
            temperature = 0.1
        )
        val normalized = parser.parseNormalizedTicket(analyzeStage.rawAnswer)
            ?: return failedOutcome(
                testCase = testCase,
                stageMetrics = listOf(analyzeStage.metrics),
                rawStages = listOf(analyzeStage.rawAnswer),
                reason = "Stage analyze parse failed"
            )

        val decideStage = stageExecutor.runStage(
            stageName = "decide",
            model = models.decideModel,
            systemPrompt = DecompositionPrompts.DECIDE_SYSTEM,
            userPrompt = DecompositionPrompts.decideUser(normalized),
            temperature = 0.2
        )
        val decision = parser.parseDecision(decideStage.rawAnswer)
            ?: return failedOutcome(
                testCase = testCase,
                stageMetrics = listOf(analyzeStage.metrics, decideStage.metrics),
                rawStages = listOf(analyzeStage.rawAnswer, decideStage.rawAnswer),
                reason = "Stage decide parse failed"
            )

        val formatStage = stageExecutor.runStage(
            stageName = "format",
            model = models.formatModel,
            systemPrompt = DecompositionPrompts.FORMAT_SYSTEM,
            userPrompt = DecompositionPrompts.formatUser(normalized, decision),
            temperature = 0.1
        )
        val finalResult = parser.parseFinalTriage(formatStage.rawAnswer)
            ?: parser.mergeToFinal(normalized, decision, formatStage.rawAnswer)

        return buildOutcome(
            testCase = testCase,
            variant = InferenceVariant.MULTI_STAGE,
            result = finalResult,
            stageMetrics = listOf(analyzeStage.metrics, decideStage.metrics, formatStage.metrics),
            rawStages = listOf(analyzeStage.rawAnswer, decideStage.rawAnswer, formatStage.rawAnswer)
        )
    }

    private fun failedOutcome(
        testCase: DecompositionTestCase,
        stageMetrics: List<StageMetrics>,
        rawStages: List<String>,
        reason: String
    ): TriageRunOutcome {
        val merged = stageMetrics.merge()
        return TriageRunOutcome(
            testCaseId = testCase.id,
            variant = InferenceVariant.MULTI_STAGE,
            result = null,
            formatValid = false,
            matchedFields = 0,
            expectedFields = expectedFieldCount(testCase),
            stageMetrics = stageMetrics,
            totalLatencyMs = merged.latencyMs,
            totalTokens = merged.totalTokens,
            totalCost = merged.cost,
            rawStages = rawStages + listOf("ERROR: $reason")
        )
    }
}

private fun CompactToonParser.mergeToFinal(
    normalized: NormalizedTicket,
    decision: TriageDecision,
    formatRaw: String
): TriageResult? {
    val fields = parse(formatRaw)
    val summary = fields["summary"]
        ?: normalized.cleanText.take(80)

    return TriageResult(
        category = decision.category,
        priority = decision.priority,
        action = decision.action,
        summary = summary
    )
}

internal fun buildOutcome(
    testCase: DecompositionTestCase,
    variant: InferenceVariant,
    result: TriageResult?,
    stageMetrics: List<StageMetrics>,
    rawStages: List<String>
): TriageRunOutcome {
    val merged = stageMetrics.merge()
    val expectedFields = expectedFieldCount(testCase)
    val matchedFields = countMatchedFields(testCase, result)

    return TriageRunOutcome(
        testCaseId = testCase.id,
        variant = variant,
        result = result,
        formatValid = result?.formatValid() == true,
        matchedFields = matchedFields,
        expectedFields = expectedFields,
        stageMetrics = stageMetrics,
        totalLatencyMs = merged.latencyMs,
        totalTokens = merged.totalTokens,
        totalCost = merged.cost,
        rawStages = rawStages
    )
}

internal fun expectedFieldCount(testCase: DecompositionTestCase): Int =
    listOfNotNull(
        testCase.expectedCategory,
        testCase.expectedPriority,
        testCase.expectedAction
    ).size

internal fun countMatchedFields(testCase: DecompositionTestCase, result: TriageResult?): Int {
    if (result == null) {
        return 0
    }

    var matched = 0
    if (testCase.expectedCategory != null &&
        result.category == testCase.expectedCategory.lowercase()
    ) {
        matched++
    }
    if (testCase.expectedPriority != null &&
        result.priority.label == testCase.expectedPriority.lowercase()
    ) {
        matched++
    }
    if (testCase.expectedAction != null &&
        result.action.label == testCase.expectedAction.lowercase()
    ) {
        matched++
    }
    return matched
}
