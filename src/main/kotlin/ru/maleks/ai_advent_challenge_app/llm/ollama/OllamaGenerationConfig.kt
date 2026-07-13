package ru.maleks.ai_advent_challenge_app.llm.ollama

data class OllamaGenerationConfig(
    val name: String,
    val options: OllamaOptions,
    val think: Boolean,
    val keepAlive: String = "10m"
)