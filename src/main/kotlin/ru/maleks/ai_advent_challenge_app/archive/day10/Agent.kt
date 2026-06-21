package ru.maleks.ai_advent_challenge_app.archive.day10

interface Agent {

    val name: String

    suspend fun handle(
        userInput: String
    ): String
}