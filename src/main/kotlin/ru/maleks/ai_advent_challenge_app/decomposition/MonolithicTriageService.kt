package ru.maleks.ai_advent_challenge_app.decomposition

class MonolithicTriageService(
    private val stageExecutor: StageExecutor,
    private val parser: CompactToonParser = CompactToonParser(),
    private val model: String
) {

    suspend fun triage(testCase: DecompositionTestCase): TriageRunOutcome {
        val execution = stageExecutor.runStage(
            stageName = "monolithic",
            model = model,
            systemPrompt = DecompositionPrompts.MONOLITHIC_SYSTEM,
            userPrompt = DecompositionPrompts.analyzeUser(testCase.ticketText)
        )

        val parsed = parser.parseFinalTriage(execution.rawAnswer)

        return buildOutcome(
            testCase = testCase,
            variant = InferenceVariant.MONOLITHIC,
            result = parsed,
            stageMetrics = listOf(execution.metrics),
            rawStages = listOf(execution.rawAnswer)
        )
    }
}
