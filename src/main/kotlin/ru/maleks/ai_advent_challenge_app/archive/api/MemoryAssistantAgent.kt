package ru.maleks.ai_advent_challenge_app.archive.api

class MemoryAssistantAgent(
    apiKey: String,
    private val memory: ConversationMemory = ConversationMemory(),
    private val tokenCounter: TokenCounter = TokenCounter()
) {

    private val llmClient = LLMApiClient(apiKey)

    private val systemMessage = OpenRouterMessage(
        role = "system",
        content = "You are a helpful AI agent with persistent conversation memory."
    )

    private val messages: ConversationState = memory.load().apply {
        this.messages.ifEmpty { this.messages = mutableListOf(systemMessage) }
    }

    suspend fun handle(userInput: String, model: String): String {
        val userMessage = OpenRouterMessage("user", userInput)

        val currentRequestTokens = tokenCounter.estimateTextTokens(userInput)
        val historyTokensBefore = tokenCounter.estimateMessagesTokens(messages.messages)

        messages.messages.add(userMessage)

        val response = llmClient.send(prepareRequest(messages.messages, model))
        println("Agent got response: $response")
        val answer = response.choices
            .firstOrNull()
            ?.message
            ?.content
            ?: "No content in response"

        val assistantMessage = OpenRouterMessage("assistant", answer)
        messages.messages.add(assistantMessage)
        memory.save(messages)

        val responseTokens = tokenCounter.estimateTextTokens(answer)

        println()
        println("----- TOKEN STATS -----")
        println("Current request estimated tokens: $currentRequestTokens")
        println("History estimated tokens before request: $historyTokensBefore")
        println("Response estimated tokens: $responseTokens")
        println("API prompt tokens: ${response.usage?.promptTokens}")
        println("API completion tokens: ${response.usage?.completionTokens}")
        println("API total tokens: ${response.usage?.totalTokens}")
        println("API cost: ${response.usage?.cost}")
        println("-----------------------")
        println()

        return answer
    }

    fun printStats() {
        println("Messages in memory: ${messages.messages.size}")
        println("Estimated history tokens: ${tokenCounter.estimateMessagesTokens(messages.messages)}")
    }

    fun addLongContext() {
        repeat(20) { index ->
            messages.messages.add(
                OpenRouterMessage(
                    role = "user",
                    content = "This is long context block number $index. " +
                            "Kotlin Spring Boot Kafka PostgreSQL microservices architecture ".repeat(100)
                )
            )
        }
        memory.save(messages)
    }

    fun addOverflowContext() {
        repeat(300) { index ->
            messages.messages.add(
                OpenRouterMessage(
                    role = "user",
                    content = "Overflow block $index. " +
                            "Very long artificial text for context window overflow demonstration. ".repeat(200)
                )
            )
        }
        memory.save(messages)
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
        messages.messages.clear()
        messages.messages.add(systemMessage)
        memory.save(messages)
    }
}