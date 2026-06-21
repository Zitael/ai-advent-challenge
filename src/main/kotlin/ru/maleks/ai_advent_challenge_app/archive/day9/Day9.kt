package ru.maleks.ai_advent_challenge_app.archive.day9

import ru.maleks.ai_advent_challenge_app.archive.api.CompressingMemoryAgent

class Day9(apiKey: String) {

    val agent = CompressingMemoryAgent(apiKey)

    suspend fun run(model: String) {
        println("AI Advent Challenge — Day 9. history compressing")
        println("Model: $model")
        println("Type your message. Type 'exit' to quit.")
        println()

        while (true) {
            print("> ")

            val userPrompt = readlnOrNull()?.trim()

            when {
                userPrompt.equals("exit", ignoreCase = true) -> break

                userPrompt.equals("clear", ignoreCase = true) -> {
                    agent.clearMemory()
                    println("Memory cleared.")
                    continue
                }

                userPrompt.equals("stats", ignoreCase = true) -> {
                    agent.printStats()
                    continue
                }
            }

            if (userPrompt.isNullOrBlank()) continue
            if (userPrompt.equals("exit", ignoreCase = true)) break

            println()

            val response = agent.handle(userPrompt, model)

            println()
            println(response)
            println()
        }
    }
}