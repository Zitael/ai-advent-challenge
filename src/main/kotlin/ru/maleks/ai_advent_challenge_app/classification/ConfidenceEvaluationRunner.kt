package ru.maleks.ai_advent_challenge_app.classification

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class ConfidenceEvaluationRunner(
    private val orchestrator: ConfidenceOrchestrator,
    private val baselineGateway: TicketClassificationGateway
) {
    private val mapper = jacksonObjectMapper()

    suspend fun run(
        testCases: List<ClassificationTestCase>,
        model: String,
        outputPath: Path
    ): ConfidenceEvaluationReport {
        val baselineMetrics = mutableListOf<LlmCallMetrics>()
        val outcomes = mutableListOf<ClassificationOutcome>()

        testCases.forEach { testCase ->
            val (_, baselineCall) = baselineGateway.classify(testCase.ticketText)
            baselineMetrics += baselineCall

            outcomes += orchestrator.classify(testCase)
        }

        val baselineMerged = baselineMetrics.merge()
        val confidenceMerged = outcomes.map {
            LlmCallMetrics(
                latencyMs = it.totalLatencyMs,
                promptTokens = 0,
                completionTokens = 0,
                totalTokens = it.totalTokens,
                cost = it.totalCost
            )
        }.merge()

        val measurableCases = outcomes.count { it.expectedCategory != null }
        val correctAccepted = outcomes.count { it.correct == true }

        val report = ConfidenceEvaluationReport(
            model = model,
            totalCases = outcomes.size,
            acceptedCount = outcomes.count { it.finalStatus == ConfidenceStatus.OK },
            rejectedCount = outcomes.count { it.rejected },
            unsureCount = outcomes.count { it.finalStatus == ConfidenceStatus.UNSURE },
            failCount = outcomes.count { it.finalStatus == ConfidenceStatus.FAIL },
            retryCount = outcomes.count { it.retried },
            correctAcceptedCount = correctAccepted,
            measurableCases = measurableCases,
            baselineLatencyMs = baselineMerged.latencyMs,
            confidenceLatencyMs = confidenceMerged.latencyMs,
            baselineTokens = baselineMerged.totalTokens,
            confidenceTokens = confidenceMerged.totalTokens,
            baselineCost = baselineMerged.cost,
            confidenceCost = confidenceMerged.cost,
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
