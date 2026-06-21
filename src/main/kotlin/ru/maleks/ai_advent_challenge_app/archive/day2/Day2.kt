package ru.maleks.ai_advent_challenge_app.archive.day2

import ru.maleks.ai_advent_challenge_app.archive.api.LLMApiClient
import ru.maleks.ai_advent_challenge_app.archive.api.OpenRouterMessage
import ru.maleks.ai_advent_challenge_app.archive.api.OpenRouterRequest

class Day2 {

    suspend fun run(model: String, apiKey: String) {

        val api = LLMApiClient(apiKey)

        println("AI Advent Challenge — Day 2")
        println("Model: $model")

        println("The prompt is: Explain how MCP works")
        println()

        try {
            //case 1 - unlimited
            val promptUnlimited = "Explain how MCP works"

            println("Case 1 - Unlimited, prompt: $promptUnlimited")
            val resultUnlimited = api.send(prepareRequest(promptUnlimited, model))

            println()
            println(resultUnlimited)
            println()

            //case 2 - Limited by prompt
            val promptLimited = "Explain how MCP works. Requirements:\n" +
                    "                - Answer in JSON\n" +
                    "                - Maximum 20 words"

            println("Case 2 - Limited by prompt, prompt: $promptLimited")
            val resultLimited = api.send(prepareRequest(promptLimited, model))

            println()
            println(resultLimited)
            println()

            //case 3 - LimitedByApi
            val promptLimitedByApi = "Explain how MCP works. End answer with END"

            println("Case 3 - Limited by api, prompt: $promptLimitedByApi")
            val resultLimitedByApi = api.send(
                prepareRequest(promptLimited, model, 1000, listOf("END"))
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