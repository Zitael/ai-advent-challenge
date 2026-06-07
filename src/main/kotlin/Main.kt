package ru.ai_advent_app.day1

import day1.Day1
import day2.Day2
import day3.Day3
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*

suspend fun main() {
    val dotenv = dotenv {
        ignoreIfMissing = true
    }

    val apiKey = dotenv["OPENROUTER_API_KEY"]
        ?: System.getenv("OPENROUTER_API_KEY")
        ?: error("OPENROUTER_API_KEY is not set")

    val model = dotenv["OPENROUTER_MODEL"]
        ?: System.getenv("OPENROUTER_MODEL")
        ?: "openai/gpt-4o-mini"

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }
    }

    val runner = Day3()

    runner.run(model, client, apiKey)

    client.close()
}