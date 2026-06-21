package ru.maleks.ai_advent_challenge_app.archive.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*

class LLMApiClient(val apiKey: String) {

    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }
    }

    suspend fun send(
        req: OpenRouterRequest
    ): OpenRouterResponse {
        return client.post("https://openrouter.ai/api/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()
    }
}