package day1

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

class Day1 {

    suspend fun run(model: String, client: HttpClient, apiKey: String) {
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
    }
}