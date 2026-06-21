package ru.maleks.ai_advent_challenge_app.archive.day10

import ru.maleks.ai_advent_challenge_app.archive.api.OpenRouterMessage
import ru.maleks.ai_advent_challenge_app.archive.api.TokenCounter

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