package ru.maleks.ai_advent_challenge_app.llm.ollama

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.system.measureTimeMillis

class OllamaClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val model: String
) {

    suspend fun complete(prompt: String): OllamaDemoResult {
        return complete(
            prompt = prompt,
            config = OllamaOptimizationProfiles.baseline
        )
    }

    suspend fun complete(
        prompt: String,
        config: OllamaGenerationConfig
    ): OllamaDemoResult {
        var response: OllamaChatResponse? = null

        val clientDurationMillis = measureTimeMillis {
            response = httpClient.post("$baseUrl/api/chat") {
                contentType(ContentType.Application.Json)

                setBody(
                    OllamaChatRequest(
                        model = model,
                        messages = listOf(
                            OllamaMessage(
                                role = "user",
                                content = prompt
                            )
                        ),
                        stream = false,
                        think = config.think,
                        options = config.options,
                        keep_alive = config.keepAlive
                    )
                )
            }.body()
        }

        val actualResponse = checkNotNull(response) {
            "Ollama returned no response"
        }

        return OllamaDemoResult(
            profile = config.name,
            prompt = prompt,
            answer = actualResponse.message.content.ifBlank {
                "Ollama returned an empty answer"
            },
            model = actualResponse.model.ifBlank { model },
            clientDurationMillis = clientDurationMillis,
            totalDurationMillis = actualResponse.totalDuration.toMillis(),
            loadDurationMillis = actualResponse.loadDuration.toMillis(),
            promptTokens = actualResponse.promptEvalCount,
            generatedTokens = actualResponse.evalCount,
            tokensPerSecond = calculateTokensPerSecond(
                tokenCount = actualResponse.evalCount,
                durationNanoseconds = actualResponse.evalDuration
            )
        )
    }

    private fun Long?.toMillis(): Long? {
        return this?.div(1_000_000)
    }

    private fun calculateTokensPerSecond(
        tokenCount: Int?,
        durationNanoseconds: Long?
    ): Double? {
        if (
            tokenCount == null ||
            durationNanoseconds == null ||
            durationNanoseconds <= 0
        ) {
            return null
        }

        val durationSeconds = durationNanoseconds / 1_000_000_000.0

        return tokenCount / durationSeconds
    }
}