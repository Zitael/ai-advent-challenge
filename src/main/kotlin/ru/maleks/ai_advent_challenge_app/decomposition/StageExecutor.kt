package ru.maleks.ai_advent_challenge_app.decomposition

import io.ktor.client.HttpClient
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterClient
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterMessage
import kotlin.system.measureTimeMillis

class StageExecutor(
    private val httpClient: HttpClient,
    private val apiKey: String
) {
    suspend fun runStage(
        stageName: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Double = 0.2
    ): StageExecutionResult {
        val client = OpenRouterClient(
            httpClient = httpClient,
            apiKey = apiKey,
            model = model
        )

        var rawAnswer = ""
        var metrics = StageMetrics(
            stageName = stageName,
            model = model,
            latencyMs = 0,
            promptTokens = 0,
            completionTokens = 0,
            totalTokens = 0,
            cost = null
        )

        val latencyMs = measureTimeMillis {
            val result = client.complete(
                messages = listOf(
                    OpenRouterMessage(role = "system", content = systemPrompt),
                    OpenRouterMessage(role = "user", content = userPrompt)
                ),
                temperature = temperature
            )
            rawAnswer = result.answer
            val usage = result.usage
            val promptTokens = usage?.promptTokens ?: 0
            val completionTokens = usage?.completionTokens ?: 0
            metrics = StageMetrics(
                stageName = stageName,
                model = model,
                latencyMs = 0,
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                totalTokens = usage?.totalTokens ?: promptTokens + completionTokens,
                cost = usage?.cost
            )
        }

        return StageExecutionResult(
            stageName = stageName,
            model = model,
            rawAnswer = rawAnswer,
            metrics = metrics.copy(latencyMs = latencyMs)
        )
    }
}

data class StageExecutionResult(
    val stageName: String,
    val model: String,
    val rawAnswer: String,
    val metrics: StageMetrics
)

internal fun List<StageMetrics>.merge(): StageMetrics {
    if (isEmpty()) {
        return StageMetrics("total", "n/a", 0, 0, 0, 0, null)
    }

    return StageMetrics(
        stageName = "total",
        model = "mixed",
        latencyMs = sumOf { it.latencyMs },
        promptTokens = sumOf { it.promptTokens },
        completionTokens = sumOf { it.completionTokens },
        totalTokens = sumOf { it.totalTokens },
        cost = mapNotNull { it.cost }.takeIf { it.isNotEmpty() }?.sum()
    )
}
