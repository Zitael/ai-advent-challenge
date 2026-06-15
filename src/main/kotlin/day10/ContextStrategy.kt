package ru.ai_advent_app.day1.day10

import api.OpenRouterMessage
import api.TokenCounter

interface ContextStrategy {
    val name: String

    suspend fun onUserMessage(message: OpenRouterMessage)
    suspend fun onAssistantMessage(message: OpenRouterMessage)

    fun buildContext(): List<OpenRouterMessage>
    fun printStats(tokenCounter: TokenCounter)
}