package ru.maleks.ai_advent_challenge_app.agent

interface Agent {
    val name: String
    suspend fun handle(userInput: String): String
}