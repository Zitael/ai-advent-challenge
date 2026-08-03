package ru.maleks.ai_advent_challenge_app.decomposition

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class DecompositionEvaluationRunner(
    private val monolithicService: MonolithicTriageService,
    private val multiStagePipeline: MultiStageTriagePipeline
) {
    private val mapper = jacksonObjectMapper()

    suspend fun run(
        testCases: List<DecompositionTestCase>,
        monolithicModel: String,
        models: MultiStageModelConfig,
        outputPath: Path
    ): DecompositionEvaluationReport {
        val outcomes = mutableListOf<TriageRunOutcome>()

        testCases.forEach { testCase ->
            outcomes += monolithicService.triage(testCase)
            outcomes += multiStagePipeline.triage(testCase)
        }

        val monolithicOutcomes = outcomes.filter { it.variant == InferenceVariant.MONOLITHIC }
        val multiStageOutcomes = outcomes.filter { it.variant == InferenceVariant.MULTI_STAGE }
        val expectedFieldChecks = testCases.sumOf { expectedFieldCount(it) }

        val report = DecompositionEvaluationReport(
            monolithicModel = monolithicModel,
            analyzeModel = models.analyzeModel,
            decideModel = models.decideModel,
            formatModel = models.formatModel,
            totalCases = testCases.size,
            monolithicValidCount = monolithicOutcomes.count { it.formatValid },
            multiStageValidCount = multiStageOutcomes.count { it.formatValid },
            monolithicMatchedFields = monolithicOutcomes.sumOf { it.matchedFields },
            multiStageMatchedFields = multiStageOutcomes.sumOf { it.matchedFields },
            expectedFieldChecks = expectedFieldChecks,
            monolithicLatencyMs = monolithicOutcomes.sumOf { it.totalLatencyMs },
            multiStageLatencyMs = multiStageOutcomes.sumOf { it.totalLatencyMs },
            monolithicTokens = monolithicOutcomes.sumOf { it.totalTokens },
            multiStageTokens = multiStageOutcomes.sumOf { it.totalTokens },
            monolithicCost = monolithicOutcomes.mapNotNull { it.totalCost }.takeIf { it.isNotEmpty() }?.sum(),
            multiStageCost = multiStageOutcomes.mapNotNull { it.totalCost }.takeIf { it.isNotEmpty() }?.sum(),
            outcomes = outcomes
        )

        Files.createDirectories(outputPath.parent)
        Files.writeString(
            outputPath,
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report),
            StandardCharsets.UTF_8
        )

        return report
    }
}
