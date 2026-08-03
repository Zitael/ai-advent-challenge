package ru.maleks.ai_advent_challenge_app.dataset

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

    val model = dotenv["BASELINE_MODEL"]
        ?: System.getenv("BASELINE_MODEL")
        ?: "openai/gpt-4o-mini"

    val projectRoot = Path.of(".").toAbsolutePath().normalize()
    val datasetDirectory = projectRoot.resolve("dataset")
    val evalPath = datasetDirectory.resolve("eval.jsonl")
    val baselinePath = datasetDirectory.resolve("baseline.json")

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { jackson() }
    }

    try {
        val runner = BaselineRunner(
            llmClient = OpenRouterClient(
                httpClient = httpClient,
                apiKey = apiKey,
                model = model
            ),
            model = model
        )

        System.out.println("AI Advent Challenge — Day 6")
        System.out.println("Baseline Runner")
        System.out.println("Model: $model")
        System.out.println()

        val report = runner.run(
            evalPath = evalPath,
            outputPath = baselinePath
        )

        System.out.println("Samples: ${report.sampleCount}")
        System.out.println("Exact matches: ${report.exactMatches}/${report.sampleCount}")
        System.out.println("Format valid: ${report.formatValidCount}/${report.sampleCount}")
        System.out.println("Output: $baselinePath")
    } finally {
        httpClient.close()
    }
}
