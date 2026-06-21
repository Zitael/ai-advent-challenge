package ru.maleks.ai_advent_challenge_app.archive.api

class CompressingMemoryAgent(
    apiKey: String,
    private val memory: ConversationMemory = ConversationMemory(),
    private val tokenCounter: TokenCounter = TokenCounter(),
    private val keepLastMessages: Int = 4,
    private val compressEveryMessages: Int = 5
) {

    private val llmClient = LLMApiClient(apiKey)
    private val systemMessage = OpenRouterMessage(
        role = "system",
        content = """
            You are a helpful AI agent with compressed persistent memory.
            Use the conversation summary and recent messages to answer follow-up questions.
        """.trimIndent()
    )

    private var state: ConversationState = memory.load()

    suspend fun handle(userInput: String, model: String): String {
        state.messages.add(OpenRouterMessage("user", userInput))

        compressIfNeeded(model)

        val requestMessages = buildRequestMessages()

        val beforeTokens = tokenCounter.estimateMessagesTokens(state.messages)
        val afterTokens = tokenCounter.estimateMessagesTokens(requestMessages)

        val result = llmClient.send(prepareRequest(requestMessages, model))
        val answer = result.choices
            .firstOrNull()
            ?.message
            ?.content
            ?: "No content in response"

        state.messages.add(OpenRouterMessage("assistant", answer))
        memory.save(state)

        println()
        println("----- CONTEXT COMPRESSION STATS -----")
        println("Stored recent messages: ${state.messages.size}")
        println("Summary exists: ${state.summary.isNotBlank()}")
        println("Raw history estimated tokens: $beforeTokens")
        println("Compressed request estimated tokens: $afterTokens")
        println("API prompt tokens: ${result.usage?.promptTokens}")
        println("API completion tokens: ${result.usage?.completionTokens}")
        println("API total tokens: ${result.usage?.totalTokens}")
        println("API cost: ${result.usage?.cost}")
        println("-------------------------------------")
        println()

        return answer
    }

    private suspend fun compressIfNeeded(model: String) {
        if (state.messages.size < keepLastMessages + compressEveryMessages) {
            return
        }

        val messagesToCompress = state.messages.dropLast(keepLastMessages)
        if (messagesToCompress.isEmpty()) {
            return
        }

        println("${messagesToCompress.size} messages to compress")

        val compressionPrompt = """
            Update the conversation summary.
            
            Existing summary:
            ${state.summary.ifBlank { "No previous summary." }}
            
            New messages to summarize:
            ${messagesToCompress.joinToString("\n") { "${it.role}: ${it.content}" }}
            
            Requirements:
            - Keep important facts, decisions, preferences and unresolved tasks.
            - Remove small talk and duplicated details.
            - Keep the summary concise but useful for future context.
        """.trimIndent()

        val summaryResult = llmClient.send(prepareRequest(
            listOf(
                OpenRouterMessage("system", "You summarize conversation history for an AI agent."),
                OpenRouterMessage("user", compressionPrompt)
            ), model
        ))

        state.summary = summaryResult.choices
            .firstOrNull()
            ?.message
            ?.content
            ?: "No content in response"
        state.messages = state.messages
            .takeLast(keepLastMessages)
            .toMutableList()

        memory.save(state)
    }

    private fun buildRequestMessages(): List<OpenRouterMessage> {
        val result = mutableListOf(systemMessage)

        if (state.summary.isNotBlank()) {
            result.add(
                OpenRouterMessage(
                    role = "system",
                    content = """
                        Conversation summary:
                        ${state.summary}
                    """.trimIndent()
                )
            )
        }

        result.addAll(state.messages)

        return result
    }

    fun printStats() {
        val requestMessages = buildRequestMessages()

        println("Summary:")
        println(state.summary.ifBlank { "No summary yet." })
        println()
        println("Recent messages: ${state.messages.size}")
        println("Raw recent messages estimated tokens: ${tokenCounter.estimateMessagesTokens(state.messages)}")
        println("Compressed request estimated tokens: ${tokenCounter.estimateMessagesTokens(requestMessages)}")
    }

    fun prepareRequest(
        messages: List<OpenRouterMessage>,
        model: String,
        maxTokens: Int? = null,
        stop: List<String>? = null,
        temperature: Double? = null
    ): OpenRouterRequest {
        return OpenRouterRequest(
            model,
            messages,
            maxTokens,
            stop,
            temperature
        )
    }

    fun clearMemory() {
        state = ConversationState()
        memory.save(state)
    }
}