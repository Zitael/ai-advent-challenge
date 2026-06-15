package ru.ai_advent_app.day1.day10

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson

class Day10(val apiKey: String) {

    suspend fun run(model: String) {

        val client: HttpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                jackson()
            }
        }

        val llmClient = OpenRouterClient(client, apiKey, model)

        val sliding = SlidingWindowStrategy(keepLastMessages = 5)
        val facts = FactsMemoryStrategy(llmClient = llmClient, keepLastMessages = 5)
        val branching = BranchingStrategy()

        val agent = StrategyAgent(
            name = "StrategyAgent",
            llmClient = llmClient,
            strategy = sliding
        )

        println("AI Advent Challenge — Day 10. strategies")
        println("Model: $model")
        println("Type your message. Type 'exit' to quit.")
        println()

        while (true) {
            print("> ")

            val input = readlnOrNull()!!.trim()

            when {
                input.equals("strategy sliding", ignoreCase = true) -> {
                    agent.setStrategy(sliding)
                    continue
                }

                input.equals("strategy facts", ignoreCase = true) -> {
                    agent.setStrategy(facts)
                    continue
                }

                input.equals("strategy branching", ignoreCase = true) -> {
                    agent.setStrategy(branching)
                    continue
                }

                input.startsWith("checkpoint ") -> {
                    val branchName = input.removePrefix("checkpoint ").trim()
                    branching.checkpoint(branchName)
                    println("Checkpoint created: $branchName")
                    continue
                }

                input.startsWith("switch ") -> {
                    val branchName = input.removePrefix("switch ").trim()
                    branching.switchBranch(branchName)
                    println("Switched to branch: $branchName")
                    continue
                }

                input.equals("branches", ignoreCase = true) -> {
                    branching.listBranches()
                    continue
                }

                input.equals("stats", ignoreCase = true) -> {
                    agent.printStats()
                    continue
                }

                input.equals("exit", ignoreCase = true) -> break
            }

            println()

            val response = agent.handle(input)

            println()
            println(response)
            println()
        }
    }
}