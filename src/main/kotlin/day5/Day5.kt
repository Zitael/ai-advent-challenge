package ru.ai_advent_app.day1.day5

import api.LLMApiClient
import api.OpenRouterMessage
import api.OpenRouterRequest
import api.OpenRouterResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class Day5 {

    data class ModelCase(
        val label: String,
        val model: String,
        val link: String
    )

    val models = listOf(
        ModelCase(
            label = "Weak",
            model = "qwen/qwen3-8b",
            link = "https://openrouter.ai/qwen/qwen3-8b"
        ),
        ModelCase(
            label = "Medium",
            model = "openai/gpt-4o-mini",
            link = "https://openrouter.ai/openai/gpt-4o-mini"
        ),
        ModelCase(
            label = "Strong",
            model = "anthropic/claude-sonnet-4",
            link = "https://openrouter.ai/anthropic/claude-sonnet-4"
        )
    )

    data class BenchmarkResult(
        val label: String,
        val model: String,
        val link: String,
        val durationMs: Long,
        val promptTokens: Int?,
        val completionTokens: Int?,
        val totalTokens: Int?,
        val cost: Double?,
        val answer: String
    )

    suspend fun askModel(
        client: HttpClient,
        apiKey: String,
        modelCase: ModelCase,
        prompt: String
    ): BenchmarkResult {
        val response: OpenRouterResponse
        val durationMs = kotlin.system.measureTimeMillis {
            response = client.post("https://openrouter.ai/api/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(prepareRequest(prompt, modelCase.model, 400, null, 0.2))
            }.body()
        }

        return BenchmarkResult(
            label = modelCase.label,
            model = modelCase.model,
            link = modelCase.link,
            durationMs = durationMs,
            promptTokens = response.usage?.promptTokens,
            completionTokens = response.usage?.completionTokens,
            totalTokens = response.usage?.totalTokens,
            cost = response.usage?.cost,
            answer = response.choices.firstOrNull()?.message?.content ?: "No content"
        )
    }

    suspend fun run(model: String, client: HttpClient, apiKey: String) {

        println("AI Advent Challenge — Day 5")

        val prompt = """
            Design a simple backend architecture for an AI assistant that helps developers analyze production incidents.
            Include:
                - main components
                - data flow
                - possible risks
                - final recommendation
    
            Keep the answer concise.
        """.trimIndent()

        println()

        try {
            val results = models.map { modelCase ->
                println("Asking ${modelCase.model}")
                askModel(client, apiKey, modelCase, prompt)
            }

            results.forEach { result ->
                println()
                println("================ ${result.label}: ${result.model} ================")
                println("Link: ${result.link}")
                println("Time: ${result.durationMs} ms")
                println("Prompt tokens: ${result.promptTokens}")
                println("Completion tokens: ${result.completionTokens}")
                println("Total tokens: ${result.totalTokens}")
                println("Cost: ${result.cost}")
                println()
                println(result.answer)
            }

        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }

    fun prepareRequest(
        prompt: String,
        model: String,
        maxTokens: Int? = null,
        stop: List<String>? = null,
        temperature: Double? = null
    ): OpenRouterRequest {
        return OpenRouterRequest(
            model,
            listOf(OpenRouterMessage(role = "user", content = prompt)),
            maxTokens,
            stop,
            temperature
        )
    }
}