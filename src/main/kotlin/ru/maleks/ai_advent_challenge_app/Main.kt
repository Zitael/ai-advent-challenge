package ru.maleks.ai_advent_challenge_app

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import ru.maleks.ai_advent_challenge_app.archive.day10.Day10

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

    val runner = Day10(apiKey)

    runner.run(model)

    client.close()
}