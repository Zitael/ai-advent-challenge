package ru.maleks.ai_advent_challenge_app.llm.ollama

data class OllamaDemoResult(
    val profile: String,
    val prompt: String,
    val answer: String,
    val model: String,
    val clientDurationMillis: Long,
    val totalDurationMillis: Long?,
    val loadDurationMillis: Long?,
    val promptTokens: Int?,
    val generatedTokens: Int?,
    val tokensPerSecond: Double?
)