package ru.maleks.ai_advent_challenge_app.llm.ollama

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
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

        defaultRequest {
            url(baseUrl)
        }

        expectSuccess = true
    }

    val ollamaClient = OllamaClient(
        httpClient = httpClient,
        baseUrl = baseUrl.removeSuffix("/"),
        model = model
    )

    val prompts = listOf(
        DemoPrompt(
            title = "Simple knowledge question",
            complexity = "simple",
            text = "Кратко объясни, что такое MCP в контексте AI-агентов."
        ),
        DemoPrompt(
            title = "Programming task",
            complexity = "medium",
            text = """
                Напиши на Kotlin функцию, которая группирует список строк
                по первой букве без учёта регистра.
                Верни полный код функции и кратко объясни сложность алгоритма.
            """.trimIndent()
        ),
        DemoPrompt(
            title = "Analytical reasoning",
            complexity = "complex",
            text = """
                Объясни, почему добавление RAG может уменьшить галлюцинации,
                хотя веса LLM при этом вообще не меняются.

                Отдельно укажи:
                1. какую роль играет retrieval;
                2. что именно попадает в prompt;
                3. почему RAG всё равно не гарантирует абсолютную точность.
            """.trimIndent()
        )
    )

    val results = mutableListOf<DemoExecution>()

    println("AI Advent Challenge - Day 26")
    println("Ollama URL: $baseUrl")
    println("Model: $model")
    println("Requests: ${prompts.size}")
    println()

    try {
        prompts.forEachIndexed { index, prompt ->
            println(
                "Running request ${index + 1}/${prompts.size}: " +
                        "${prompt.title} [${prompt.complexity}]"
            )

            val execution = try {
                DemoExecution.Success(
                    prompt = prompt,
                    result = ollamaClient.complete(prompt.text)
                )
            } catch (exception: Exception) {
                DemoExecution.Failure(
                    prompt = prompt,
                    error = exception.message
                        ?: exception::class.simpleName
                        ?: "Unknown error"
                )
            }

            results += execution

            when (execution) {
                is DemoExecution.Success -> {
                    println(
                        "Completed in ${execution.result.clientDurationMillis} ms; " +
                                "generated tokens=${execution.result.generatedTokens ?: "unknown"}"
                    )
                }

                is DemoExecution.Failure -> {
                    println("Failed: ${execution.error}")
                }
            }

            println()
        }

        val reportPath = saveReport(
            baseUrl = baseUrl,
            configuredModel = model,
            executions = results
        )

        println("Demo completed.")
        println("Successful requests: ${results.count { it is DemoExecution.Success }}")
        println("Failed requests: ${results.count { it is DemoExecution.Failure }}")
        println("Report: ${reportPath.toAbsolutePath()}")
    } finally {
        httpClient.close()
    }
}

private data class DemoPrompt(
    val title: String,
    val complexity: String,
    val text: String
)

private sealed interface DemoExecution {

    val prompt: DemoPrompt

    data class Success(
        override val prompt: DemoPrompt,
        val result: OllamaDemoResult
    ) : DemoExecution

    data class Failure(
        override val prompt: DemoPrompt,
        val error: String
    ) : DemoExecution
}

private fun saveReport(
    baseUrl: String,
    configuredModel: String,
    executions: List<DemoExecution>
): Path {
    val outputDirectory = Path.of("ollama-output")
    Files.createDirectories(outputDirectory)

    val resultSections = executions
        .mapIndexed { index, execution ->
            when (execution) {
                is DemoExecution.Success -> {
                    buildSuccessSection(
                        number = index + 1,
                        execution = execution
                    )
                }

                is DemoExecution.Failure -> {
                    buildFailureSection(
                        number = index + 1,
                        execution = execution
                    )
                }
            }
        }
        .joinToString("\n\n---\n\n")

    val report = """
        # Day 26 — Local LLM with Ollama

        ## Configuration

        - Ollama API: `$baseUrl`
        - configured model: `$configuredModel`
        - requests: ${executions.size}
        - successful: ${executions.count { it is DemoExecution.Success }}
        - failed: ${executions.count { it is DemoExecution.Failure }}

        ## Result

        The model was called through the local Ollama HTTP API.
        All requests were executed on the local machine without OpenRouter.

        $resultSections
    """.trimIndent()

    val path = outputDirectory.resolve("day26-ollama-demo.md")
    path.writeText(report, Charsets.UTF_8)

    return path
}

private fun buildSuccessSection(
    number: Int,
    execution: DemoExecution.Success
): String {
    val result = execution.result

    return """
        ## Request $number — ${execution.prompt.title}

        - complexity: ${execution.prompt.complexity}
        - model returned by Ollama: `${result.model}`
        - client duration: ${result.clientDurationMillis} ms
        - Ollama total duration: ${result.totalDurationMillis.formatMillis()}
        - model load duration: ${result.loadDurationMillis.formatMillis()}
        - prompt tokens: ${result.promptTokens ?: "unknown"}
        - generated tokens: ${result.generatedTokens ?: "unknown"}
        - generation speed: ${result.tokensPerSecond.formatSpeed()}

        ### Prompt

        ${execution.prompt.text}

        ### Answer

        ${result.answer}
    """.trimIndent()
}

private fun buildFailureSection(
    number: Int,
    execution: DemoExecution.Failure
): String {
    return """
        ## Request $number — ${execution.prompt.title}

        - complexity: ${execution.prompt.complexity}
        - status: failed
        - error: ${execution.error}

        ### Prompt

        ${execution.prompt.text}
    """.trimIndent()
}

private fun Long?.formatMillis(): String {
    return this?.let { "$it ms" } ?: "unknown"
}

private fun Double?.formatSpeed(): String {
    return this?.let { "%.2f tokens/sec".format(it) } ?: "unknown"
}