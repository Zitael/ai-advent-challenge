package ru.maleks.ai_advent_challenge_app.archive.api

class Agent(
    apiKey: String,
    val memory: ConversationMemory = ConversationMemory()
) {

    private val api = LLMApiClient(apiKey)

    private val systemMessage = OpenRouterMessage(
        role = "system",
        content = """
            You are a helpful AI agent with persistent conversation memory.
            Use previous messages to answer follow-up questions.
            Answer clearly and concisely.
        """.trimIndent()
    )

    private val messages: ConversationState = memory.load().apply {
        this.messages.ifEmpty { this.messages = mutableListOf(systemMessage) }
    }

    suspend fun callLLM(model: String, userPrompt: String): String {
        messages.messages.add(OpenRouterMessage("user", userPrompt))
        println("Agent - called with $userPrompt")

        val response = api.send(prepareRequest(messages.messages, model))

        println("Agent - got result: $response")

        val answer = response.choices
            .firstOrNull()
            ?.message
            ?.content
            ?: "No content in response"

        messages.messages.add(
            OpenRouterMessage(
                role = "assistant",
                content = answer
            )
        )

        memory.save(messages)

        return answer
    }

    fun clearMemory() {
        messages.messages.clear()
        messages.messages.add(systemMessage)
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
}