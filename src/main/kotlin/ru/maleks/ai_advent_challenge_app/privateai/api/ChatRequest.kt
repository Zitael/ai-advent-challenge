package ru.maleks.ai_advent_challenge_app.privateai.api

data class ChatRequest(
    val sessionId: String = "",
    val message: String = ""
)