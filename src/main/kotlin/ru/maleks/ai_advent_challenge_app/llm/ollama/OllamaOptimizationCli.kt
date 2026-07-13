package ru.maleks.ai_advent_challenge_app.llm.ollama

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

suspend fun main() {
    val dotenv = dotenv {
        ignoreIfMissing = true
    }

    val baseUrl = dotenv["OLLAMA_BASE_URL"]
        ?: System.getenv("OLLAMA_BASE_URL")
        ?: "http://localhost:11434"

    val model = dotenv["OLLAMA_MODEL"]
        ?: System.getenv("OLLAMA_MODEL")
        ?: "qwen3:8b"

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 300_000
            socketTimeoutMillis = 300_000
        }

        expectSuccess = true
    }

    val client = OllamaClient(
        httpClient = httpClient,
        baseUrl = baseUrl.removeSuffix("/"),
        model = model
    )

    val promptBuilder = RagOptimizationPromptBuilder()

    val question = """
        Чем MCP отличается от RAG и как они могут работать вместе?
    """.trimIndent()

    val context = """
        Source: 02_mcp.md
        Section: Что такое MCP

        MCP публикует инструменты, доступные агенту.
        Инструмент содержит название, описание, параметры и результат.
        MCP может использоваться для вызова REST API, SQL, Git и локальных функций.

        Source: 03_rag.md
        Section: Что такое RAG

        RAG дополняет знания модели внешними документами.
        Во время запроса выполняется поиск релевантных чанков,
        после чего найденные документы добавляются в prompt модели.
    """.trimIndent()

    println("AI Advent Challenge - Day 29")
    println("Model: $model")
    println("Running baseline and optimized profiles...")
    println()

    try {
        warmUp(client)

        val baselineResult = client.complete(
            prompt = promptBuilder.baseline(
                question = question,
                context = context
            ),
            config = OllamaOptimizationProfiles.baseline
        )

        println(
            "Baseline completed: " +
                    "${baselineResult.clientDurationMillis} ms, " +
                    "${baselineResult.generatedTokens ?: "unknown"} tokens"
        )

        val optimizedResult = client.complete(
            prompt = promptBuilder.optimized(
                question = question,
                context = context
            ),
            config = OllamaOptimizationProfiles.optimizedRag
        )

        println(
            "Optimized completed: " +
                    "${optimizedResult.clientDurationMillis} ms, " +
                    "${optimizedResult.generatedTokens ?: "unknown"} tokens"
        )

        val reportPath = saveReport(
            model = model,
            baseline = baselineResult,
            optimized = optimizedResult
        )

        println()
        println("Report: ${reportPath.toAbsolutePath()}")
    } finally {
        httpClient.close()
    }
}

private suspend fun warmUp(client: OllamaClient) {
    println("Warming up model...")

    client.complete(
        prompt = "Ответь одним словом: готов.",
        config = OllamaGenerationConfig(
            name = "warmup",
            options = OllamaOptions(
                temperature = 0.0,
                num_predict = 10,
                num_ctx = 2048
            ),
            think = false,
            keepAlive = "10m"
        )
    )

    println("Warm-up completed.")
    println()
}

private fun saveReport(
    model: String,
    baseline: OllamaDemoResult,
    optimized: OllamaDemoResult
): Path {
    val outputDirectory = Path.of("ollama-output")
    Files.createDirectories(outputDirectory)

    val durationDifference =
        baseline.clientDurationMillis - optimized.clientDurationMillis

    val generatedTokenDifference =
        (baseline.generatedTokens ?: 0) -
                (optimized.generatedTokens ?: 0)

    val report = """
        # Day 29 — Local LLM optimization

        ## Model

        `$model`

        ## Target task

        Corporate RAG question answering with concise grounded answers.

        ## Baseline configuration

        - temperature: 0.7
        - max generated tokens: 700
        - context window: 4096
        - thinking: enabled
        - generic prompt

        ## Optimized configuration

        - temperature: 0.2
        - max generated tokens: 350
        - context window: 8192
        - thinking: disabled
        - specialized grounded RAG prompt
        - maximum requested answer length: 150 words
        - mandatory source section
        - explicit unknown-answer rule

        ## Baseline result

        - duration: ${baseline.clientDurationMillis} ms
        - prompt tokens: ${baseline.promptTokens ?: "unknown"}
        - generated tokens: ${baseline.generatedTokens ?: "unknown"}
        - speed: ${baseline.tokensPerSecond.formatSpeed()}

        ### Answer

        ${baseline.answer}

        ## Optimized result

        - duration: ${optimized.clientDurationMillis} ms
        - prompt tokens: ${optimized.promptTokens ?: "unknown"}
        - generated tokens: ${optimized.generatedTokens ?: "unknown"}
        - speed: ${optimized.tokensPerSecond.formatSpeed()}

        ### Answer

        ${optimized.answer}

        ## Comparison

        - duration difference: $durationDifference ms
        - generated token difference: $generatedTokenDifference
        - baseline answer length: ${baseline.answer.length} characters
        - optimized answer length: ${optimized.answer.length} characters

        ## Quality checklist

        Baseline:

        - contains answer section: ${baseline.answer.contains("## Ответ")}
        - contains sources section: ${baseline.answer.contains("## Источники")}
        - refuses unsupported information: not guaranteed

        Optimized:

        - contains answer section: ${optimized.answer.contains("## Ответ")}
        - contains sources section: ${optimized.answer.contains("## Источники")}
        - concise output: ${optimized.answer.length <= 2_000}
        - task-specific prompt: enabled
        - unknown-answer rule: enabled
    """.trimIndent()

    val path = outputDirectory.resolve("day29-ollama-optimization.md")
    path.writeText(report, Charsets.UTF_8)

    return path
}

private fun Double?.formatSpeed(): String {
    return this?.let {
        "%.2f tokens/sec".format(it)
    } ?: "unknown"
}