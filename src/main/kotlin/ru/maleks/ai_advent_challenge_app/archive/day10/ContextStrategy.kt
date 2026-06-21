package ru.maleks.ai_advent_challenge_app.archive.day10

import ru.maleks.ai_advent_challenge_app.archive.api.OpenRouterMessage
import ru.maleks.ai_advent_challenge_app.archive.api.TokenCounter

interface ContextStrategy {
    val name: String

    suspend fun onUserMessage(message: OpenRouterMessage)
    suspend fun onAssistantMessage(message: OpenRouterMessage)

    fun buildContext(): List<OpenRouterMessage>
    fun printStats(tokenCounter: TokenCounter)
}