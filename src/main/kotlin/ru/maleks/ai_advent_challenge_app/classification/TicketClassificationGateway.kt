package ru.maleks.ai_advent_challenge_app.classification

import ru.maleks.ai_advent_challenge_app.dataset.DatasetSystemPrompt
import ru.maleks.ai_advent_challenge_app.llm.LlmClient
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterMessage
import kotlin.system.measureTimeMillis

class TicketClassificationGateway(
    private val llmClient: LlmClient
) {
    suspend fun classify(
        ticketText: String,
        temperature: Double = 0.3
    ): Pair<String, LlmCallMetrics> {
        var metrics = LlmCallMetrics(
            latencyMs = 0,
            promptTokens = 0,
            completionTokens = 0,
            totalTokens = 0,
            cost = null
        )
        var answer = ""

        val latencyMs = measureTimeMillis {
            val result = llmClient.complete(
                messages = listOf(
                    OpenRouterMessage(
                        role = "system",
                        content = DatasetSystemPrompt.TICKET_CLASSIFIER
                    ),
                    OpenRouterMessage(
                        role = "user",
                        content = DatasetSystemPrompt.userContent(ticketText)
                    )
                ),
                temperature = temperature
            )
            answer = result.answer
            metrics = result.usage.toMetrics()
        }

        return answer to metrics.copy(latencyMs = latencyMs)
    }
}

internal fun ru.maleks.ai_advent_challenge_app.llm.Usage?.toMetrics(): LlmCallMetrics {
    val promptTokens = this?.promptTokens ?: 0
    val completionTokens = this?.completionTokens ?: 0
    val totalTokens = this?.totalTokens ?: promptTokens + completionTokens

    return LlmCallMetrics(
        latencyMs = 0,
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens,
        cost = this?.cost
    )
}

internal fun List<LlmCallMetrics>.merge(): LlmCallMetrics =
    LlmCallMetrics(
        latencyMs = sumOf { it.latencyMs },
        promptTokens = sumOf { it.promptTokens },
        completionTokens = sumOf { it.completionTokens },
        totalTokens = sumOf { it.totalTokens },
        cost = mapNotNull { it.cost }.takeIf { it.isNotEmpty() }?.sum()
    )

internal operator fun LlmCallMetrics.plus(other: LlmCallMetrics): LlmCallMetrics =
    LlmCallMetrics(
        latencyMs = latencyMs + other.latencyMs,
        promptTokens = promptTokens + other.promptTokens,
        completionTokens = completionTokens + other.completionTokens,
        totalTokens = totalTokens + other.totalTokens,
        cost = listOfNotNull(cost, other.cost).takeIf { it.isNotEmpty() }?.sum()
    )
