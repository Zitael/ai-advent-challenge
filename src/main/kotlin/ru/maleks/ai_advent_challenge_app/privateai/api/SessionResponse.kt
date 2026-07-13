package ru.maleks.ai_advent_challenge_app.privateai.api

data class SessionResponse(
    val sessionId: String,
    val historyMessages: Int,
    val message: String
)