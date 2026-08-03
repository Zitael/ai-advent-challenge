package ru.maleks.ai_advent_challenge_app.micromodel

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import ru.maleks.ai_advent_challenge_app.classification.TicketClassificationGateway
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterClient
import java.nio.file.Path

suspend fun main() {
    val dotenv = dotenv { ignoreIfMissing = true }

    val apiKey = dotenv["OPENROUTER_API_KEY"]
        ?: System.getenv("OPENROUTER_API_KEY")
        ?: error("OPENROUTER_API_KEY is not set")

    val fallbackModel = dotenv["MICRO_FALLBACK_MODEL"]
        ?: System.getenv("MICRO_FALLBACK_MODEL")
        ?: "openai/gpt-4o"

    val confidenceThreshold = dotenv["MICRO_CONFIDENCE_THRESHOLD"]
        ?: System.getenv("MICRO_CONFIDENCE_THRESHOLD")
        ?: "0.65"

    val threshold = confidenceThreshold.toDoubleOrNull()
        ?: error("MICRO_CONFIDENCE_THRESHOLD must be a number")

    val projectRoot = Path.of(".").toAbsolutePath().normalize()
    val micromodelDirectory = projectRoot.resolve("micromodel")
    val testCasesPath = micromodelDirectory.resolve("test-cases.json")
    val reportPath = micromodelDirectory.resolve("report.json")

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { jackson() }
    }

    try {
        val fallbackGateway = TicketClassificationGateway(
            llmClient = OpenRouterClient(
                httpClient = httpClient,
                apiKey = apiKey,
                model = fallbackModel
            )
        )

        val pipeline = MicroFirstPipeline(
            microClassifier = TicketKeywordMicroClassifier(),
            decisionPolicy = MicroModelDecisionPolicy(
                confidenceThreshold = threshold
            ),
            fallbackGateway = fallbackGateway
        )

        val testCases = MicromodelTestCaseLoader().load(testCasesPath)
        require(testCases.size >= 20) {
            "Need at least 20 test cases, got ${testCases.size}"
        }

        val runner = MicromodelEvaluationRunner(pipeline)

        System.out.println("AI Advent Challenge — Day 10")
        System.out.println("Micro-model First Pipeline")
        System.out.println("Micro-model: keyword-rule-classifier (local)")
        System.out.println("Fallback model: $fallbackModel")
        System.out.println("Confidence threshold: $threshold")
        System.out.println("Cases: ${testCases.size}")
        System.out.println()

        val report = runner.run(
            testCases = testCases,
            microModelName = "keyword-rule-classifier",
            fallbackModel = fallbackModel,
            confidenceThreshold = threshold,
            outputPath = reportPath
        )

        printSummary(report, reportPath)
    } finally {
        httpClient.close()
    }
}

private fun printSummary(report: MicromodelEvaluationReport, reportPath: Path) {
    System.out.println("Micro-model handled: ${report.microHandledCount}/${report.totalCases}")
    System.out.println("Fallback used: ${report.fallbackCount}/${report.totalCases}")
    System.out.println("LLM calls: ${report.llmCallCount}")
    System.out.println("Correct via micro: ${report.microCorrectCount}")
    System.out.println("Correct via fallback: ${report.fallbackCorrectCount}")
    System.out.println()
    System.out.println("Avg latency: ${report.avgLatencyMs} ms")
    System.out.println("Micro avg latency: ${report.microAvgLatencyMs} ms")
    System.out.println("Fallback avg latency: ${report.fallbackAvgLatencyMs} ms")
    System.out.println("Estimated always-LLM latency: ${report.estimatedAlwaysLlmLatencyMs} ms")
    System.out.println("Latency saved: ${report.latencySavedMs} ms")
    report.totalFallbackCost?.let { System.out.println("Fallback cost: $it") }
    System.out.println()
    System.out.println("By kind:")
    MicromodelTestKind.entries.forEach { kind ->
        val subset = report.outcomes.filter { it.kind == kind }
        val micro = subset.count { it.handledByMicro }
        System.out.println("- $kind: micro=$micro/${subset.size}, fallback=${subset.size - micro}")
    }
    System.out.println()
    System.out.println("Report: $reportPath")
}
