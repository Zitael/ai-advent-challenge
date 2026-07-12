package ru.maleks.ai_advent_challenge_app.llm.ollama

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class OllamaMessage(
    val role: String = "",
    val content: String = ""
)