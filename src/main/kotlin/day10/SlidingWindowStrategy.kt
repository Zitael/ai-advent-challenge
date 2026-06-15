package ru.ai_advent_app.day1.day10

import api.OpenRouterMessage
import api.TokenCounter

class SlidingWindowStrategy(
    private val keepLastMessages: Int = 8
) : ContextStrategy {

    override val name = "SlidingWindow"

    private val messages = mutableListOf<OpenRouterMessage>()

    override suspend fun onUserMessage(message: OpenRouterMessage) {
        messages.add(message)
        trim()
    }

    override suspend fun onAssistantMessage(message: OpenRouterMessage) {
        messages.add(message)
        trim()
    }

    override fun buildContext(): List<OpenRouterMessage> {
        return messages.takeLast(keepLastMessages)
    }

    private fun trim() {
        while (messages.size > keepLastMessages) {
            messages.removeAt(0)
        }
    }

    override fun printStats(tokenCounter: TokenCounter) {
        println("Strategy: $name")
        println("Stored messages: ${messages.size}")
        println("Context tokens: ${tokenCounter.estimateMessagesTokens(buildContext())}")
    }
}