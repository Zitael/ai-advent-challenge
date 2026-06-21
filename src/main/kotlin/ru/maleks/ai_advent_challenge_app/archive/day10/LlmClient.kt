package ru.maleks.ai_advent_challenge_app.archive.day10

import ru.maleks.ai_advent_challenge_app.archive.api.OpenRouterMessage

interface LlmClient {

    suspend fun complete(
        messages: List<OpenRouterMessage>
    ): LlmResult
}