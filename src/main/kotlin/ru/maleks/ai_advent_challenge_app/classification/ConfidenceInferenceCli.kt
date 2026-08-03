package ru.maleks.ai_advent_challenge_app.classification

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterClient
import java.nio.file.Path

suspend fun main() {
    val dotenv = dotenv { ignoreIfMissing = true }

    val apiKey = dotenv["OPENROUTER_API_KEY"]
        ?: System.getenv("OPENROUTER_API_KEY")
        ?: error("OPENROUTER_API_KEY is not set")

    val model = dotenv["CONFIDENCE_MODEL"]
        ?: System.getenv("CONFIDENCE_MODEL")
        ?: "openai/gpt-4o-mini"

    val projectRoot = Path.of(".").toAbsolutePath().normalize()
    val classificationDirectory = projectRoot.resolve("classification")
    val testCasesPath = classificationDirectory.resolve("test-cases.json")
    val reportPath = classificationDirectory.resolve("report.json")

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { jackson() }
    }

    try {
        val llmClient = OpenRouterClient(
            httpClient = httpClient,
            apiKey = apiKey,
            model = model
        )
        val gateway = TicketClassificationGateway(llmClient)

        val orchestrator = ConfidenceOrchestrator(
            scoringClassifier = ScoringClassifier(llmClient),
            constraintValidator = ConstraintValidator(),
            redundancyChecker = RedundancyChecker(gateway),
            selfCheckVerifier = SelfCheckVerifier(llmClient)
        )

        val testCases = ClassificationTestCaseLoader().load(testCasesPath)
        val runner = ConfidenceEvaluationRunner(
            orchestrator = orchestrator,
            baselineGateway = gateway
        )

        System.out.println("AI Advent Challenge — Day 7")
        System.out.println("Confidence-Controlled Ticket Classification")
        System.out.println("Model: $model")
        System.out.println("Cases: ${testCases.size}")
        System.out.println()

        val report = runner.run(
            testCases = testCases,
            model = model,
            outputPath = reportPath
        )

        printSummary(report, reportPath)
    } finally {
        httpClient.close()
    }
}

private fun printSummary(report: ConfidenceEvaluationReport, reportPath: Path) {
    System.out.println("Accepted (OK): ${report.acceptedCount}/${report.totalCases}")
    System.out.println("Rejected: ${report.rejectedCount}/${report.totalCases}")
    System.out.println("UNSURE: ${report.unsureCount}/${report.totalCases}")
    System.out.println("FAIL: ${report.failCount}/${report.totalCases}")
    System.out.println("Retried: ${report.retryCount}/${report.totalCases}")
    System.out.println("Correct accepted: ${report.correctAcceptedCount}/${report.measurableCases}")
    System.out.println()
    System.out.println("Baseline latency: ${report.baselineLatencyMs} ms")
    System.out.println("Confidence latency: ${report.confidenceLatencyMs} ms")
    System.out.println("Latency multiplier: ${latencyMultiplier(report)}x")
    System.out.println()
    System.out.println("Baseline tokens: ${report.baselineTokens}")
    System.out.println("Confidence tokens: ${report.confidenceTokens}")
    System.out.println("Token multiplier: ${tokenMultiplier(report)}x")
    report.baselineCost?.let { System.out.println("Baseline cost: $it") }
    report.confidenceCost?.let { System.out.println("Confidence cost: $it") }
    System.out.println()
    System.out.println("Report: $reportPath")
}

private fun latencyMultiplier(report: ConfidenceEvaluationReport): String {
    if (report.baselineLatencyMs == 0L) {
        return "n/a"
    }
    return "%.1f".format(report.confidenceLatencyMs.toDouble() / report.baselineLatencyMs)
}

private fun tokenMultiplier(report: ConfidenceEvaluationReport): String {
    if (report.baselineTokens == 0) {
        return "n/a"
    }
    return "%.1f".format(report.confidenceTokens.toDouble() / report.baselineTokens)
}
