package ru.ai_advent_app.day1.day10

interface Agent {

    val name: String

    suspend fun handle(
        userInput: String
    ): String
}