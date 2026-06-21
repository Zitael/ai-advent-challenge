package ru.maleks.ai_advent_challenge_app.archive.day4

import ru.maleks.ai_advent_challenge_app.archive.api.LLMApiClient
import ru.maleks.ai_advent_challenge_app.archive.api.OpenRouterMessage
import ru.maleks.ai_advent_challenge_app.archive.api.OpenRouterRequest

class Day4 {

    suspend fun run(model: String, apiKey: String) {

        val api = LLMApiClient(apiKey)

        println("AI Advent Challenge — Day 4")
        println("Model: $model")

        val prompt = """
            Come up with an idea for an AI assistant for software developers.
            Give a short name and one sentence description.
        """.trimIndent()

        println("Temperature, task: $prompt")
        println()

        try {
            val temperatures = listOf(0.0, 0.7, 1.2)

            for (temperature in temperatures) {
                val answer = api.send(prepareRequest(prompt, model, temperature = temperature))

                println()
                println("================ TEMPERATURE $temperature ================")
                println(answer)
            }

        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
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