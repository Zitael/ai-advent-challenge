package ru.maleks.ai_advent_challenge_app.llm

data class LlmResult(
    val answer: String,
    val usage: Usage?
)