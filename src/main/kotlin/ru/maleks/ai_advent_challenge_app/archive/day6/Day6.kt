package ru.maleks.ai_advent_challenge_app.archive.day6

import ru.maleks.ai_advent_challenge_app.archive.api.Agent

class Day6(apiKey: String) {

    val agent = Agent(apiKey)

    suspend fun run(model: String) {
        println("AI Advent Challenge — Day 6. calling by agent")
        println("Model: $model")
        println("Type your message. Type 'exit' to quit.")
        println()

        while (true) {
            print("> ")
            val userPrompt = readlnOrNull()?.trim()

            if (userPrompt.isNullOrBlank()) continue
            if (userPrompt.equals("exit", ignoreCase = true)) break

                println()

                val response = agent.callLLM(model, userPrompt)

                println()
                println(response)
                println()
        }
    }
}