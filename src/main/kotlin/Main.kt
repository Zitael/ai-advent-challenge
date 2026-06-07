package ru.ai_advent_app

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import ru.ai_advent_app.dto.OpenRouterMessage
import ru.ai_advent_app.dto.OpenRouterRequest
import ru.ai_advent_app.dto.OpenRouterResponse

suspend fun main() {
    val dotenv = dotenv {
        ignoreIfMissing = true
    }

    val apiKey = dotenv["OPENROUTER_API_KEY"]
        ?: System.getenv("OPENROUTER_API_KEY")
        ?: error("OPENROUTER_API_KEY is not set")

    val model = dotenv["OPENROUTER_MODEL"]
        ?: System.getenv("OPENROUTER_MODEL")
        ?: "openai/gpt-4o-mini"

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }
    }

    println("AI Advent Challenge — Day 1")
    println("Model: $model")
    println("Type your message. Type 'exit' to quit.")
    println()

    while (true) {
        print("> ")
        val userPrompt = readlnOrNull()?.trim()

        if (userPrompt.isNullOrBlank()) continue
        if (userPrompt.equals("exit", ignoreCase = true)) break

        try {
            val response: OpenRouterResponse =
                client.post("https://openrouter.ai/api/v1/chat/completions") {
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpenRouterRequest(
                            model = model,
                            messages = listOf(
                                OpenRouterMessage(
                                    role = "user",
                                    content = userPrompt
                                )
                            )
                        )
                    )
                }.body()

            println()

            val answer = response.choices
                .firstOrNull()
                ?.message
                ?.content
                ?: "No content in response"

            println()
            println(answer)
            println()
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }

    client.close()
}