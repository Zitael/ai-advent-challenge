package ru.maleks.ai_advent_challenge_app.llm.ollama

data class OllamaOptions(
    val temperature: Double = 0.7,
    val num_predict: Int = 700
)