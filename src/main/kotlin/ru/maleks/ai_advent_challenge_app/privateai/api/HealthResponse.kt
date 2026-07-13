package ru.maleks.ai_advent_challenge_app.privateai.api

data class HealthResponse(
    val status: String,
    val service: String,
    val ollama: String,
    val model: String
)