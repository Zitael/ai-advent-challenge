package ru.maleks.ai_advent_challenge_app.rag.chat

data class RagChatMessage(
    val role: String,
    val content: String,
    val createdAt: String
)