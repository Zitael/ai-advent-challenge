package day2

import api.LLMApiClient
import api.OpenRouterMessage
import io.ktor.client.HttpClient
import api.OpenRouterRequest

class Day2 {

    suspend fun run(model: String, client: HttpClient, apiKey: String) {

        val api = LLMApiClient()

        println("AI Advent Challenge — Day 2")
        println("Model: $model")

        println("The prompt is: Explain how MCP works")
        println()

        try {
            //case 1 - unlimited
            val promptUnlimited = "Explain how MCP works"

            println("Case 1 - Unlimited, prompt: $promptUnlimited")
            val resultUnlimited = api.send(client, prepareRequest(promptUnlimited, model), apiKey)

            println()
            println(resultUnlimited)
            println()

            //case 2 - Limited by prompt
            val promptLimited = "Explain how MCP works. Requirements:\n" +
                    "                - Answer in JSON\n" +
                    "                - Maximum 20 words"

            println("Case 2 - Limited by prompt, prompt: $promptLimited")
            val resultLimited = api.send(client, prepareRequest(promptLimited, model), apiKey)

            println()
            println(resultLimited)
            println()

            //case 3 - LimitedByApi
            val promptLimitedByApi = "Explain how MCP works. End answer with END"

            println("Case 3 - Limited by api, prompt: $promptLimitedByApi")
            val resultLimitedByApi = api.send(
                client,
                prepareRequest(promptLimited, model, 1000, listOf("END")),
                apiKey
            )

            println()
            println(resultLimitedByApi)
            println()


        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }

    suspend fun prepareRequest(
        prompt: String,
        model: String,
        maxTokens: Int? = null,
        stop: List<String>? = null,
    ): OpenRouterRequest {
        return OpenRouterRequest(
            model,
            listOf(OpenRouterMessage(role = "user", content = prompt)),
            maxTokens,
            stop
        )
    }
}