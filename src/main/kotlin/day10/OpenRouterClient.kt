package ru.ai_advent_app.day1.day10

import api.OpenRouterMessage
import api.OpenRouterRequest
import api.OpenRouterResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class OpenRouterClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val model: String
) : LlmClient {

    override suspend fun complete(
        messages: List<OpenRouterMessage>
    ): LlmResult {
        val response: OpenRouterResponse =
            httpClient.post("https://openrouter.ai/api/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(
                    OpenRouterRequest(
                        model = model,
                        messages = messages,
                        maxTokens = 700,
                        temperature = 0.7
                    )
                )
            }.body()

        return LlmResult(
            answer = response.choices
                .firstOrNull()
                ?.message
                ?.content
                ?: "No content in response",
            usage = response.usage
        )
    }
}