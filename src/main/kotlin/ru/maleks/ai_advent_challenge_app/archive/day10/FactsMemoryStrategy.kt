package ru.maleks.ai_advent_challenge_app.archive.day10

import ru.maleks.ai_advent_challenge_app.archive.api.OpenRouterMessage
import ru.maleks.ai_advent_challenge_app.archive.api.TokenCounter

class FactsMemoryStrategy(
    private val llmClient: LlmClient,
    private val keepLastMessages: Int = 8
) : ContextStrategy {

    override val name = "FactsMemory"

    private val messages = mutableListOf<OpenRouterMessage>()
    private val facts = linkedMapOf<String, String>()

    override suspend fun onUserMessage(message: OpenRouterMessage) {
        messages.add(message)
        updateFacts(message.content.orEmpty())
        trim()
    }

    override suspend fun onAssistantMessage(message: OpenRouterMessage) {
        messages.add(message)
        trim()
    }

    override fun buildContext(): List<OpenRouterMessage> {
        val result = mutableListOf<OpenRouterMessage>()

        if (facts.isNotEmpty()) {
            result.add(
                OpenRouterMessage(
                    role = "system",
                    content = """
                        Important persistent facts:
                        ${facts.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }}
                    """.trimIndent()
                )
            )
        }

        result.addAll(messages.takeLast(keepLastMessages))
        return result
    }

    private suspend fun updateFacts(userMessage: String) {
        val prompt = """
            Extract important long-term facts from the user message.
            Return only key=value lines.
            If there are no important facts, return EMPTY.
            
            Important facts include:
            - user goal
            - requirements
            - constraints
            - preferences
            - decisions
            
            User message:
            $userMessage
        """.trimIndent()

        val result = llmClient.complete(
            listOf(
                OpenRouterMessage("system", "You extract structured memory facts."),
                OpenRouterMessage("user", prompt)
            )
        )

        val answer = result.answer.trim()

        if (answer.equals("EMPTY", ignoreCase = true)) {
            return
        }

        answer.lines()
            .map { it.trim() }
            .filter { it.contains("=") }
            .forEach { line ->
                val parts = line.split("=", limit = 2)
                val key = parts[0].trim()
                val value = parts[1].trim()

                if (key.isNotBlank() && value.isNotBlank()) {
                    facts[key] = value
                }
            }
    }

    private fun trim() {
        while (messages.size > keepLastMessages) {
            messages.removeAt(0)
        }
    }

    override fun printStats(tokenCounter: TokenCounter) {
        println("Strategy: $name")
        println("Facts: ${facts.size}")
        facts.forEach { (key, value) -> println("- $key: $value") }
        println("Recent messages: ${messages.size}")
        println("Context tokens: ${tokenCounter.estimateMessagesTokens(buildContext())}")
    }
}