package ru.maleks.ai_advent_challenge_app.llm.ollama

data class OllamaOptions(
    val temperature: Double = 0.7,
    val num_predict: Int = 700,
    val num_ctx: Int = 4096,
    val top_p: Double = 0.9,
    val repeat_penalty: Double = 1.1
)