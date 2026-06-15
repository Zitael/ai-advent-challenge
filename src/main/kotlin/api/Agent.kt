package api

class Agent(val apiKey: String) {

    private val api = LLMApiClient(apiKey)

    suspend fun callLLM(model: String, userPrompt: String): String {
        println("Agent - called with $userPrompt")
        val response: OpenRouterResponse = api.send(prepareRequest(userPrompt, model))
        println("Agent - got result: $response")

        return response
            .choices
            .firstOrNull()
            ?.message
            ?.content
            ?: "No content in response"
    }

    fun prepareRequest(
        prompt: String,
        model: String,
        maxTokens: Int? = null,
        stop: List<String>? = null,
        temperature: Double? = null
    ): OpenRouterRequest {
        return OpenRouterRequest(
            model,
            listOf(OpenRouterMessage(role = "user", content = prompt)),
            maxTokens,
            stop,
            temperature
        )
    }
}