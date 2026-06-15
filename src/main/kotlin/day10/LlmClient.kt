package ru.ai_advent_app.day1.day10

import api.OpenRouterMessage

interface LlmClient {

    suspend fun complete(
        messages: List<OpenRouterMessage>
    ): LlmResult
}