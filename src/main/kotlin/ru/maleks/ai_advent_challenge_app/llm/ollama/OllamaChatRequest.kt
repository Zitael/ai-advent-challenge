package ru.maleks.ai_advent_challenge_app.llm.ollama

data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean = false,
    val options: OllamaOptions = OllamaOptions(),
    val keep_alive: String = "10m"
)