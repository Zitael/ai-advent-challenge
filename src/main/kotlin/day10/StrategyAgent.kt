package ru.ai_advent_app.day1.day10

import api.OpenRouterMessage
import api.TokenCounter

class StrategyAgent(
    override val name: String,
    private val llmClient: LlmClient,
    private var strategy: ContextStrategy,
    private val tokenCounter: TokenCounter = TokenCounter()
) : Agent {

    private val systemMessage = OpenRouterMessage(
        role = "system",
        content = """
            You are a helpful AI agent.
            Use provided context carefully.
            If facts are available, treat them as important persistent memory.
        """.trimIndent()
    )

    override suspend fun handle(userInput: String): String {
        val userMessage = OpenRouterMessage("user", userInput)

        strategy.onUserMessage(userMessage)

        val requestMessages = listOf(systemMessage) + strategy.buildContext()

        val result = llmClient.complete(requestMessages)

        val assistantMessage = OpenRouterMessage("assistant", result.answer)
        strategy.onAssistantMessage(assistantMessage)

        println()
        println("----- CONTEXT STRATEGY STATS -----")
        println("Strategy: ${strategy.name}")
        println("Request tokens estimated: ${tokenCounter.estimateMessagesTokens(requestMessages)}")
        println("API prompt tokens: ${result.usage?.promptTokens}")
        println("API completion tokens: ${result.usage?.completionTokens}")
        println("API total tokens: ${result.usage?.totalTokens}")
        println("API cost: ${result.usage?.cost}")
        println("----------------------------------")
        println()

        return result.answer
    }

    fun setStrategy(newStrategy: ContextStrategy) {
        strategy = newStrategy
        println("Strategy switched to: ${strategy.name}")
    }

    fun getStrategy(): ContextStrategy = strategy

    fun printStats() {
        strategy.printStats(tokenCounter)
    }
}