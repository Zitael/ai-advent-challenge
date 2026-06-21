package ru.maleks.ai_advent_challenge_app.archive.day8

import ru.maleks.ai_advent_challenge_app.archive.api.MemoryAssistantAgent

class Day8(apiKey: String) {

    val agent = MemoryAssistantAgent(apiKey)

    suspend fun run(model: String) {
        println("AI Advent Challenge — Day 8. token statistics")
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

                userPrompt.equals("long", ignoreCase = true) -> {
                    agent.addLongContext()
                    println("Long context added.")
                    continue
                }

                userPrompt.equals("overflow", ignoreCase = true) -> {
                    agent.addOverflowContext()
                    println("Overflow context added.")
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