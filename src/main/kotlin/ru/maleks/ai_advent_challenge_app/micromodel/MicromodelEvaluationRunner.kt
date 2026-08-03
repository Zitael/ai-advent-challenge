package ru.maleks.ai_advent_challenge_app.micromodel

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class MicromodelEvaluationRunner(
    private val pipeline: MicroFirstPipeline
) {
    private val mapper = jacksonObjectMapper()

    suspend fun run(
        testCases: List<MicromodelTestCase>,
        microModelName: String,
        fallbackModel: String,
        confidenceThreshold: Double,
        outputPath: Path
    ): MicromodelEvaluationReport {
        val outcomes = testCases.map { pipeline.classify(it) }

        val microHandled = outcomes.filter { it.handledByMicro }
        val fallback = outcomes.filter { it.fallbackUsed }
        val measurableCases = outcomes.count { it.expectedCategory != null }

        val avgLatency = if (outcomes.isNotEmpty()) {
            outcomes.sumOf { it.totalLatencyMs } / outcomes.size
        } else {
            0L
        }
        val microAvgLatency = if (microHandled.isNotEmpty()) {
            microHandled.sumOf { it.microLatencyMs } / microHandled.size
        } else {
            0L
        }
        val fallbackAvgLatency = if (fallback.isNotEmpty()) {
            fallback.sumOf { it.fallbackLatencyMs } / fallback.size
        } else {
            0L
        }

        val estimatedAlwaysLlmLatency = fallbackAvgLatency * outcomes.size
        val actualLatency = outcomes.sumOf { it.totalLatencyMs }
        val latencySaved = (estimatedAlwaysLlmLatency - actualLatency).coerceAtLeast(0)

        val report = MicromodelEvaluationReport(
            microModelName = microModelName,
            fallbackModel = fallbackModel,
            confidenceThreshold = confidenceThreshold,
            totalCases = outcomes.size,
            microHandledCount = microHandled.size,
            fallbackCount = fallback.size,
            llmCallCount = fallback.size,
            microCorrectCount = microHandled.count { it.correct == true },
            fallbackCorrectCount = fallback.count { it.correct == true },
            measurableCases = measurableCases,
            avgLatencyMs = avgLatency,
            microAvgLatencyMs = microAvgLatency,
            fallbackAvgLatencyMs = fallbackAvgLatency,
            totalFallbackTokens = fallback.sumOf { it.fallbackTokens },
            totalFallbackCost = fallback.mapNotNull { it.fallbackCost }.takeIf { it.isNotEmpty() }?.sum(),
            estimatedAlwaysLlmLatencyMs = estimatedAlwaysLlmLatency,
            latencySavedMs = latencySaved,
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
