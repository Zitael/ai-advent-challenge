package ru.maleks.ai_advent_challenge_app.decomposition

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import java.nio.file.Path

suspend fun main() {
    val dotenv = dotenv { ignoreIfMissing = true }

    val apiKey = dotenv["OPENROUTER_API_KEY"]
        ?: System.getenv("OPENROUTER_API_KEY")
        ?: error("OPENROUTER_API_KEY is not set")

    val monolithicModel = dotenv["DECOMP_MONOLITHIC_MODEL"]
        ?: System.getenv("DECOMP_MONOLITHIC_MODEL")
        ?: "openai/gpt-4o-mini"

    val analyzeModel = dotenv["DECOMP_ANALYZE_MODEL"]
        ?: System.getenv("DECOMP_ANALYZE_MODEL")
        ?: "openai/gpt-4o-mini"

    val decideModel = dotenv["DECOMP_DECIDE_MODEL"]
        ?: System.getenv("DECOMP_DECIDE_MODEL")
        ?: "openai/gpt-4o"

    val formatModel = dotenv["DECOMP_FORMAT_MODEL"]
        ?: System.getenv("DECOMP_FORMAT_MODEL")
        ?: "openai/gpt-4o-mini"

    val projectRoot = Path.of(".").toAbsolutePath().normalize()
    val decompositionDirectory = projectRoot.resolve("decomposition")
    val testCasesPath = decompositionDirectory.resolve("test-cases.json")
    val reportPath = decompositionDirectory.resolve("report.json")

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { jackson() }
    }

    try {
        val stageExecutor = StageExecutor(
            httpClient = httpClient,
            apiKey = apiKey
        )
        val models = MultiStageModelConfig(
            analyzeModel = analyzeModel,
            decideModel = decideModel,
            formatModel = formatModel
        )

        val runner = DecompositionEvaluationRunner(
            monolithicService = MonolithicTriageService(
                stageExecutor = stageExecutor,
                model = monolithicModel
            ),
            multiStagePipeline = MultiStageTriagePipeline(
                stageExecutor = stageExecutor,
                models = models
            )
        )

        val testCases = DecompositionTestCaseLoader().load(testCasesPath)

        System.out.println("AI Advent Challenge — Day 9")
        System.out.println("Inference Decomposition")
        System.out.println("Monolithic model: $monolithicModel")
        System.out.println("Multi-stage models: analyze=$analyzeModel, decide=$decideModel, format=$formatModel")
        System.out.println("Cases: ${testCases.size}")
        System.out.println()

        val report = runner.run(
            testCases = testCases,
            monolithicModel = monolithicModel,
            models = models,
            outputPath = reportPath
        )

        printSummary(report, reportPath)
    } finally {
        httpClient.close()
    }
}

private fun printSummary(report: DecompositionEvaluationReport, reportPath: Path) {
    System.out.println("Variant A — monolithic")
    System.out.println("  Valid format: ${report.monolithicValidCount}/${report.totalCases}")
    System.out.println("  Matched fields: ${report.monolithicMatchedFields}/${report.expectedFieldChecks}")
    System.out.println("  Latency: ${report.monolithicLatencyMs} ms")
    System.out.println("  Tokens: ${report.monolithicTokens}")
    report.monolithicCost?.let { System.out.println("  Cost: $it") }
    System.out.println()
    System.out.println("Variant B — multi-stage")
    System.out.println("  Valid format: ${report.multiStageValidCount}/${report.totalCases}")
    System.out.println("  Matched fields: ${report.multiStageMatchedFields}/${report.expectedFieldChecks}")
    System.out.println("  Latency: ${report.multiStageLatencyMs} ms")
    System.out.println("  Tokens: ${report.multiStageTokens}")
    report.multiStageCost?.let { System.out.println("  Cost: $it") }
    System.out.println()
    System.out.println("Per-case comparison:")
    report.outcomes
        .groupBy { it.testCaseId }
        .forEach { (caseId, runs) ->
            val mono = runs.first { it.variant == InferenceVariant.MONOLITHIC }
            val multi = runs.first { it.variant == InferenceVariant.MULTI_STAGE }
            System.out.println(
                "- $caseId: mono fields=${mono.matchedFields}/${mono.expectedFields}, " +
                    "multi fields=${multi.matchedFields}/${multi.expectedFields}, " +
                    "multi stages=${multi.stageMetrics.size}"
            )
        }
    System.out.println()
    System.out.println("Report: $reportPath")
}
