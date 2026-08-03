package ru.maleks.ai_advent_challenge_app.routing

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class RoutingEvaluationRunner(
    private val router: ModelRouter
) {
    private val mapper = jacksonObjectMapper()

    suspend fun run(
        testCases: List<RoutingTestCase>,
        cheapModel: String,
        strongModel: String,
        outputPath: Path
    ): RoutingEvaluationReport {
        val outcomes = testCases.map { testCase ->
            router.route(testCase)
        }

        val cheapOnly = outcomes.filter { !it.escalated }
        val escalated = outcomes.filter { it.escalated }

        val report = RoutingEvaluationReport(
            cheapModel = cheapModel,
            strongModel = strongModel,
            totalCases = outcomes.size,
            cheapOnlyCount = cheapOnly.size,
            escalatedCount = escalated.size,
            correctCount = outcomes.count { it.correct == true },
            measurableCases = outcomes.count { it.expectedCategory != null },
            cheapOnlyLatencyMs = cheapOnly.sumOf { it.totalLatencyMs },
            escalatedLatencyMs = escalated.sumOf { it.totalLatencyMs },
            totalLatencyMs = outcomes.sumOf { it.totalLatencyMs },
            cheapOnlyTokens = cheapOnly.sumOf { it.totalTokens },
            escalatedTokens = escalated.sumOf { it.totalTokens },
            totalTokens = outcomes.sumOf { it.totalTokens },
            cheapOnlyCost = cheapOnly.mapNotNull { it.totalCost }.takeIf { it.isNotEmpty() }?.sum(),
            escalatedCost = escalated.mapNotNull { it.totalCost }.takeIf { it.isNotEmpty() }?.sum(),
            totalCost = outcomes.mapNotNull { it.totalCost }.takeIf { it.isNotEmpty() }?.sum(),
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
