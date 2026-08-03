package ru.maleks.ai_advent_challenge_app.routing

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import ru.maleks.ai_advent_challenge_app.classification.ScoringClassifier
import ru.maleks.ai_advent_challenge_app.classification.TicketClassificationGateway
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterClient
import java.nio.file.Path

suspend fun main() {
    val dotenv = dotenv { ignoreIfMissing = true }

    val apiKey = dotenv["OPENROUTER_API_KEY"]
        ?: System.getenv("OPENROUTER_API_KEY")
        ?: error("OPENROUTER_API_KEY is not set")

    val cheapModel = dotenv["ROUTING_CHEAP_MODEL"]
        ?: System.getenv("ROUTING_CHEAP_MODEL")
        ?: "openai/gpt-4o-mini"

    val strongModel = dotenv["ROUTING_STRONG_MODEL"]
        ?: System.getenv("ROUTING_STRONG_MODEL")
        ?: "openai/gpt-4o"

    val projectRoot = Path.of(".").toAbsolutePath().normalize()
    val routingDirectory = projectRoot.resolve("routing")
    val testCasesPath = routingDirectory.resolve("test-cases.json")
    val reportPath = routingDirectory.resolve("report.json")

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { jackson() }
    }

    try {
        val cheapClient = OpenRouterClient(
            httpClient = httpClient,
            apiKey = apiKey,
            model = cheapModel
        )
        val strongClient = OpenRouterClient(
            httpClient = httpClient,
            apiKey = apiKey,
            model = strongModel
        )

        val router = ModelRouter(
            cheapScoringClassifier = ScoringClassifier(cheapClient),
            strongGateway = TicketClassificationGateway(strongClient)
        )

        val testCases = RoutingTestCaseLoader().load(testCasesPath)
        val runner = RoutingEvaluationRunner(router)

        System.out.println("AI Advent Challenge — Day 8")
        System.out.println("Model Routing")
        System.out.println("Cheap model: $cheapModel")
        System.out.println("Strong model: $strongModel")
        System.out.println("Cases: ${testCases.size}")
        System.out.println()

        val report = runner.run(
            testCases = testCases,
            cheapModel = cheapModel,
            strongModel = strongModel,
            outputPath = reportPath
        )

        printSummary(report, reportPath)
    } finally {
        httpClient.close()
    }
}

private fun printSummary(report: RoutingEvaluationReport, reportPath: Path) {
    System.out.println("Cheap model only: ${report.cheapOnlyCount}/${report.totalCases}")
    System.out.println("Escalated to strong: ${report.escalatedCount}/${report.totalCases}")
    System.out.println("Correct: ${report.correctCount}/${report.measurableCases}")
    System.out.println()
    System.out.println("Cheap-only latency: ${report.cheapOnlyLatencyMs} ms")
    System.out.println("Escalated latency: ${report.escalatedLatencyMs} ms")
    System.out.println("Total latency: ${report.totalLatencyMs} ms")
    System.out.println()
    System.out.println("Cheap-only tokens: ${report.cheapOnlyTokens}")
    System.out.println("Escalated tokens: ${report.escalatedTokens}")
    System.out.println("Total tokens: ${report.totalTokens}")
    report.totalCost?.let { System.out.println("Total cost: $it") }
    System.out.println()
    System.out.println("Per-case routing:")
    report.outcomes.forEach { outcome ->
        val modelLabel = if (outcome.escalated) "STRONG" else "CHEAP"
        System.out.println("- ${outcome.testCaseId}: $modelLabel -> ${outcome.acceptedCategory ?: "null"}")
    }
    System.out.println()
    System.out.println("Report: $reportPath")
}
