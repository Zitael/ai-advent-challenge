package ru.maleks.ai_advent_challenge_app.gateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterMessage
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterRequest
import ru.maleks.ai_advent_challenge_app.llm.OpenRouterResponse

class OpenRouterProxyClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val baseUrl: String = "https://openrouter.ai/api/v1"
) {

    suspend fun chat(
        model: String,
        messages: List<GatewayMessage>,
        temperature: Double?
    ): ProxyChatResult {
        val response: OpenRouterResponse = httpClient.post("$baseUrl/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(
                OpenRouterRequest(
                    model = model,
                    messages = messages.map { message ->
                        OpenRouterMessage(role = message.role, content = message.content)
                    },
                    maxTokens = 700,
                    temperature = temperature ?: 0.3
                )
            )
        }.body()

        val usage = response.usage
        val promptTokens = usage?.promptTokens ?: 0
        val completionTokens = usage?.completionTokens ?: 0

        return ProxyChatResult(
            answer = response.choices.firstOrNull()?.message?.content.orEmpty(),
            usage = GatewayUsage(
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                totalTokens = usage?.totalTokens ?: promptTokens + completionTokens
            ),
            costUsd = usage?.cost
        )
    }
}

data class ProxyChatResult(
    val answer: String,
    val usage: GatewayUsage?,
    val costUsd: Double?
)
