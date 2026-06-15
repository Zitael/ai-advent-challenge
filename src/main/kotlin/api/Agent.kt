package api

import ru.ai_advent_app.day1.api.ConversationMemory

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

    private val messages: MutableList<OpenRouterMessage> = memory.load()
        .ifEmpty {
            mutableListOf(systemMessage)
        }

    suspend fun callLLM(model: String, userPrompt: String): String {
        messages.add(OpenRouterMessage("user", userPrompt))
        println("Agent - called with $userPrompt")

        val response = api.send(prepareRequest(messages, model))

        println("Agent - got result: $response")

        val answer = response.choices
            .firstOrNull()
            ?.message
            ?.content
            ?: "No content in response"

        messages.add(
            OpenRouterMessage(
                role = "assistant",
                content = answer
            )
        )

        memory.save(messages)

        return answer
    }

    fun clearMemory() {
        messages.clear()
        messages.add(systemMessage)
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