package ru.maleks.ai_advent_challenge_app.llm.ollama

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
        return complete(
            messages = listOf(
                OllamaMessage(
                    role = "user",
                    content = prompt
                )
            ),
            config = config
        )
    }

    suspend fun complete(
        messages: List<OllamaMessage>,
        config: OllamaGenerationConfig
    ): OllamaDemoResult {
        require(messages.isNotEmpty()) {
            "At least one message is required"
        }

        val requestUrl = "$baseUrl/api/chat"

        log("Preparing Ollama request")
        log("URL: $requestUrl")
        log("Model: $model")
        log("Profile: ${config.name}")
        log("Messages: ${messages.size}")
        log("Thinking enabled: ${config.think}")
        log("Temperature: ${config.options.temperature}")
        log("Max generated tokens: ${config.options.num_predict}")
        log("Context window: ${config.options.num_ctx}")
        log("Keep alive: ${config.keepAlive}")
        log("Sending HTTP request...")

        var response: OllamaChatResponse? = null

        val clientDurationMillis = try {
            measureTimeMillis {
                response = httpClient.post(requestUrl) {
                    contentType(ContentType.Application.Json)

                    setBody(
                        OllamaChatRequest(
                            model = model,
                            messages = messages,
                            stream = false,
                            think = config.think,
                            options = config.options,
                            keep_alive = config.keepAlive
                        )
                    )
                }.body()
            }
        } catch (exception: Exception) {
            log(
                "Ollama request failed: " +
                        "${exception::class.simpleName}: " +
                        "${exception.message}"
            )

            throw exception
        }

        log(
            "HTTP response received in " +
                    "$clientDurationMillis ms"
        )

        val actualResponse = checkNotNull(response) {
            "Ollama returned no response"
        }

        log("Response model: ${actualResponse.model}")
        log("Done: ${actualResponse.done}")
        log(
            "Done reason: " +
                    "${actualResponse.doneReason ?: "unknown"}"
        )
        log(
            "Prompt tokens: " +
                    "${actualResponse.promptEvalCount ?: "unknown"}"
        )
        log(
            "Generated tokens: " +
                    "${actualResponse.evalCount ?: "unknown"}"
        )
        log(
            "Model load duration: " +
                    actualResponse.loadDuration.toMillisText()
        )
        log(
            "Prompt evaluation duration: " +
                    actualResponse.promptEvalDuration.toMillisText()
        )
        log(
            "Generation duration: " +
                    actualResponse.evalDuration.toMillisText()
        )
        log(
            "Total Ollama duration: " +
                    actualResponse.totalDuration.toMillisText()
        )

        val tokensPerSecond = calculateTokensPerSecond(
            tokenCount = actualResponse.evalCount,
            durationNanoseconds = actualResponse.evalDuration
        )

        log(
            "Generation speed: " +
                    tokensPerSecond.formatTokensPerSecond()
        )
        log(
            "Answer length: " +
                    "${actualResponse.message.content.length} chars"
        )

        return OllamaDemoResult(
            profile = config.name,
            prompt = messages.last().content,
            answer = actualResponse.message.content.ifBlank {
                "Ollama returned an empty answer"
            },
            model = actualResponse.model.ifBlank { model },
            clientDurationMillis = clientDurationMillis,
            totalDurationMillis =
                actualResponse.totalDuration.toMillis(),
            loadDurationMillis =
                actualResponse.loadDuration.toMillis(),
            promptTokens = actualResponse.promptEvalCount,
            generatedTokens = actualResponse.evalCount,
            tokensPerSecond = tokensPerSecond
        )
    }

    private fun Long?.toMillis(): Long? {
        return this?.div(1_000_000)
    }

    private fun Long?.toMillisText(): String {
        return this?.let {
            "${it / 1_000_000} ms"
        } ?: "unknown"
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

        val durationSeconds =
            durationNanoseconds / 1_000_000_000.0

        return tokenCount / durationSeconds
    }

    private fun Double?.formatTokensPerSecond(): String {
        return this?.let {
            "%.2f tokens/sec".format(it)
        } ?: "unknown"
    }

    private fun log(message: String) {
        println(
            "[${currentTimestamp()}] [OLLAMA] $message"
        )
    }

    private fun currentTimestamp(): String {
        return LocalDateTime.now().format(LOG_TIME_FORMAT)
    }

    private companion object {
        val LOG_TIME_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    }
}