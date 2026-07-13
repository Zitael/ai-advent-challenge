package ru.maleks.ai_advent_challenge_app.privateai.service

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.isSuccess

class OllamaHealthService(
    private val httpClient: HttpClient,
    private val ollamaBaseUrl: String
) {

    suspend fun isAvailable(): Boolean {
        return runCatching {
            httpClient
                .get("$ollamaBaseUrl/api/version")
                .status
                .isSuccess()
        }.getOrDefault(false)
    }
}