package ru.maleks.ai_advent_challenge_app.privateai.api

data class ChatResponse(
    val sessionId: String,
    val answer: String,
    val model: String,
    val historyMessages: Int,
    val durationMillis: Long,
    val promptTokens: Int?,
    val generatedTokens: Int?
)